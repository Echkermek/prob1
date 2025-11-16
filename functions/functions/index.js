const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendMessageNotification = functions.firestore
  .document('teacher_messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    try {
      console.log('🔔 Starting notification process...');
      
      const messageData = snapshot.data();
      const { text, recipientId, recipientName, senderId } = messageData;

      console.log(`📨 New message for recipient: ${recipientId}`);
      console.log(`📝 Message text: ${text}`);

      // 1. Получаем FCM токены получателя
      console.log(`🔍 Looking for FCM tokens for user: ${recipientId}`);
      const tokensSnapshot = await admin.firestore()
        .collection('fcm_tokens')
        .where('userId', '==', recipientId)
        .get();

      if (tokensSnapshot.empty) {
        console.log('❌ No FCM tokens found for user:', recipientId);
        return null;
      }

      const tokens = tokensSnapshot.docs.map(doc => doc.data().token);
      console.log(`✅ Found ${tokens.length} tokens for user ${recipientId}`);

      // 2. Подготавливаем уведомление
      const notificationBody = text.length > 50 ? text.substring(0, 50) + '...' : text;
      
      const payload = {
        notification: {
          title: '📩 Новое сообщение',
          body: notificationBody,
          icon: 'ic_notification',
          sound: 'default'
        },
        data: {
          type: 'message',
          messageId: context.params.messageId,
          recipientId: recipientId,
          senderId: senderId,
          title: 'Новое сообщение',
          body: text,
          click_action: 'FLUTTER_NOTIFICATION_CLICK'
        },
        android: {
          priority: 'high'
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
              badge: 1
            }
          }
        }
      };

      console.log('📤 Sending notification payload:', payload);

      // 3. Отправляем уведомление
      const response = await admin.messaging().sendToDevice(tokens, payload);
      console.log(`📨 Notification sent. Success: ${response.successCount}`);
      
      // 4. Обрабатываем результаты отправки
      const tokensToRemove = [];
      response.results.forEach((result, index) => {
        const error = result.error;
        if (error) {
          console.error(`❌ Failure sending to token ${tokens[index]}:`, error);
          
          // Удаляем невалидные токены
          if (error.code === 'messaging/invalid-registration-token' ||
              error.code === 'messaging/registration-token-not-registered') {
            tokensToRemove.push(tokensSnapshot.docs[index].ref.delete());
            console.log(`🗑️ Marked token for removal: ${tokens[index]}`);
          }
        }
      });

      // 5. Удаляем невалидные токены
      if (tokensToRemove.length > 0) {
        await Promise.all(tokensToRemove);
        console.log(`🧹 Removed ${tokensToRemove.length} invalid tokens`);
      }

      console.log('✅ Notification process completed successfully');
      return { success: true, sentCount: response.successCount };
      
    } catch (error) {
      console.error('💥 Error in notification function:', error);
      return { success: false, error: error.message };
    }
  });

// Дополнительная функция для отправки тестового уведомления
exports.sendTestNotification = functions.https.onCall(async (data, context) => {
  try {
    const { userId, message } = data;
    
    const tokensSnapshot = await admin.firestore()
      .collection('fcm_tokens')
      .where('userId', '==', userId)
      .get();

    if (tokensSnapshot.empty) {
      return { success: false, error: 'No tokens found' };
    }

    const tokens = tokensSnapshot.docs.map(doc => doc.data().token);
    
    const payload = {
      notification: {
        title: '🧪 Тестовое уведомление',
        body: message || 'Это тестовое уведомление',
        sound: 'default'
      }
    };

    const response = await admin.messaging().sendToDevice(tokens, payload);
    
    return { 
      success: true, 
      sentCount: response.successCount,
      tokens: tokens.length
    };
    
  } catch (error) {
    console.error('Test notification error:', error);
    return { success: false, error: error.message };
  }
});
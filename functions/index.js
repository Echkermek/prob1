const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendDeadlineNotification = functions.https.onCall(async (data, context) => {
  const { courseId, testId } = data;

  // Получаем название теста
  const testDoc = await admin.firestore().collection('test').doc(testId).get();
  const testTitle = testDoc.exists ? testDoc.data().title || 'тест' : 'тест';

  // Отправляем пуш всей группе (по топику)
  const message = {
    notification: {
      title: 'Дедлайн приближается!',
      body: `Сдайте "${testTitle}" — сегодня!`,
    },
    data: {
      type: 'deadline',
      courseId,
      testId,
    },
    topic: `course_${courseId}`,
  };

  await admin.messaging().send(message);
  console.log(`Пуш отправлен: ${testTitle}, course ${courseId}`);

  return { success: true };
});
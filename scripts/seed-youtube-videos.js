/**
 * Seed curated YouTube videos into Firestore (admin script).
 *
 * Usage (from repo root, with Firebase CLI logged in):
 *   node scripts/seed-youtube-videos.js
 *
 * Collection: youtube_videos
 * Fields: videoId, title, titleKannada, publishedAtMillis, active
 */
const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

const VIDEOS = [
  {
    id: 'QmEw0LO417E',
    videoId: 'QmEw0LO417E',
    title: 'EduAI learning video',
    titleKannada: 'EduAI ಕಲಿಕೆ ವೀಡಿಯೋ',
    publishedAtMillis: Date.now(),
    active: true,
  },
  {
    id: 'qTs6e_XmYNo',
    videoId: 'qTs6e_XmYNo',
    title: 'EduAI learning video',
    titleKannada: 'EduAI ಕಲಿಕೆ ವೀಡಿಯೋ',
    publishedAtMillis: Date.now() - 86_400_000,
    active: true,
  },
];

async function main() {
  const batch = db.batch();
  for (const video of VIDEOS) {
    const ref = db.collection('youtube_videos').doc(video.id);
    batch.set(ref, video, { merge: true });
  }
  await batch.commit();
  console.log(`Seeded ${VIDEOS.length} youtube_videos documents.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

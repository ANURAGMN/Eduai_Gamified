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
    id: 'hzG7pPYZYuQ',
    videoId: 'hzG7pPYZYuQ',
    title: 'Photosynthesis',
    titleKannada: 'ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ',
    publishedAtMillis: Date.parse('2026-08-15T16:54:56Z'),
    active: true,
    channelHandle: '@AnuRag-fo2ts',
    sourceUrl: 'https://www.youtube.com/shorts/hzG7pPYZYuQ',
  },
  {
    id: 'qTs6e_XmYNo',
    videoId: 'qTs6e_XmYNo',
    title: 'What If Learning Could Grow a Plant?',
    titleKannada: 'ಕಲಿಕೆಯಿಂದ ಸಸ್ಯ ಬೆಳೆಯುತ್ತದೆಯೇ?',
    publishedAtMillis: Date.parse('2026-07-31T05:44:50Z'),
    active: true,
    channelHandle: '@AnuRag-fo2ts',
    sourceUrl: 'https://www.youtube.com/watch?v=qTs6e_XmYNo',
  },
  {
    id: 'jl-vshnBrXA',
    videoId: 'jl-vshnBrXA',
    title: 'AI Teacher for Every Student',
    titleKannada: 'ಪ್ರತಿ ವಿದ್ಯಾರ್ಥಿಗೆ AI ಶಿಕ್ಷಕ',
    publishedAtMillis: Date.parse('2026-06-22T15:13:35Z'),
    active: true,
    channelHandle: '@AnuRag-fo2ts',
    sourceUrl: 'https://www.youtube.com/watch?v=jl-vshnBrXA',
  },
  {
    id: 'QmEw0LO417E',
    videoId: 'QmEw0LO417E',
    title: 'EduAI User Guide | Your Personal AI Math & Science Teacher',
    titleKannada: 'EduAI ಬಳಕೆದಾರ ಮಾರ್ಗದರ್ಶಿ',
    publishedAtMillis: Date.parse('2026-06-22T14:36:34Z'),
    active: true,
    channelHandle: '@AnuRag-fo2ts',
    sourceUrl: 'https://www.youtube.com/watch?v=QmEw0LO417E',
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

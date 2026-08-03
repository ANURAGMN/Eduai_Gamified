#!/system/bin/sh
sqlite3 databases/eduai_database <<EOF
UPDATE quest_daily SET simsClaimed=0, studyDone=1, studyClaimed=0, bonusClaimed=0 WHERE questDate='2026-07-24';
SELECT simsDone,simsTotal,simsClaimed,studyDone,studyTotal,studyClaimed,bonusClaimed FROM quest_daily WHERE questDate='2026-07-24';
EOF

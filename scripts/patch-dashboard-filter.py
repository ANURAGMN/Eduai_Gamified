#!/usr/bin/env python3
"""Patch existing dashboard.html with client-side internal-team filter + label fixes."""

from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DASH = ROOT / "reports" / "dashboard.html"
INTERNAL = {"jeecounsela@gmail.com", "nkb.rgp@gmail.com", "check@padaams.in"}
IST = timezone(timedelta(hours=5, minutes=30))


def main() -> None:
    html = DASH.read_text(encoding="utf-8")
    now = datetime.now(IST).strftime("%Y-%m-%d %H:%M IST")

    if "exclude-internal" not in html:
        html = html.replace(
            "    code { background: #eef2ff; padding: 2px 6px; border-radius: 4px; }\n  </style>",
            """    code { background: #eef2ff; padding: 2px 6px; border-radius: 4px; }
    .filter-bar { background: #fffbeb; border-bottom: 1px solid #fcd34d; padding: 12px 32px; display: flex; flex-wrap: wrap; align-items: center; gap: 16px; font-size: 14px; }
    .filter-bar label { display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 600; }
    .filter-bar .hint { color: var(--muted); font-size: 12px; }
    .quota-banner { background: #fef2f2; border-bottom: 1px solid #fecaca; color: #991b1b; padding: 10px 32px; font-size: 13px; }
  </style>""",
        )
        html = html.replace(
            "  </header>\n  <nav class=\"bucket-nav\">",
            f"""  </header>
  <div class="quota-banner">Patched {now} — Firestore read quota was exceeded; showing cached data from earlier today. Re-run <code>.\\scripts\\run-dashboard.ps1</code> later for a live pull.</div>
  <div class="filter-bar">
    <label><input type="checkbox" id="exclude-internal"/> Exclude internal team</label>
    <span class="hint">Internal: {", ".join(sorted(INTERNAL))}</span>
  </div>
  <nav class="bucket-nav">""",
        )

    html = re.sub(
        r"<p>Generated [^<]+</p>",
        f"<p>Generated {now} (patched) · Since 2026-07-09 (IST) · Firebase eduai-e090e</p>",
        html,
        count=1,
    )
    html = html.replace(
        "Total app session time",
        "Total session time (all users)",
    )
    html = html.replace(
        "Agent/sim screen time",
        "Total tutor/sim time (all users)",
    )
    html = html.replace(
        "Session time = sum of session durationMillis. Screen time = analytics EXIT duration on CHATBOT, MATH_AGENT, SIMULATION_VIEWER, SIMULATION_AGENT screens.",
        "Totals sum all users in the window — not per-user. Session time = app foreground duration. Tutor/sim time = EXIT duration on tutor/sim screens. Reports cap orphan visits at 2h after next app release.",
    )

    if "filterInternalRows" not in html:
        html = html.replace(
            "</body>",
            """  <script>
    (function() {
      const INTERNAL = new Set(""" + str(list(INTERNAL)).replace("'", '"') + """);
      const cb = document.getElementById('exclude-internal');
      if (!cb) return;

      function isInternalText(t) {
        for (const e of INTERNAL) if (t.includes(e)) return true;
        return false;
      }

      function applyFilter() {
        const hide = cb.checked;
        document.querySelectorAll('table tbody tr').forEach(tr => {
          const txt = tr.textContent || '';
          if (hide && isInternalText(txt)) tr.style.display = 'none';
          else tr.style.display = '';
        });
        document.querySelectorAll('details').forEach(d => {
          const txt = d.querySelector('summary')?.textContent || '';
          if (hide && isInternalText(txt)) d.style.display = 'none';
          else d.style.display = '';
        });
        document.querySelectorAll('section ul li').forEach(li => {
          if (hide && isInternalText(li.textContent || '')) li.style.display = 'none';
          else li.style.display = '';
        });
      }
      cb.addEventListener('change', applyFilter);
    })();
  </script>
</body>""",
        )

    DASH.write_text(html, encoding="utf-8")
    print(f"Patched: {DASH}")


if __name__ == "__main__":
    main()

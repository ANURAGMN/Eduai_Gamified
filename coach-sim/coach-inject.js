/*
 * Cognita Maths Coach — live inject bundle.
 * Paste this into the browser console while a chapter-1 math sim is open
 * (https://anuragmn.github.io/EduAI_app/Simulations/math_1_*.html), or use the bookmarklet in
 * coach-review.html. It reads the live problem, solves it, GLOWS the correct option, and shows the
 * number-specific worked feedback — the exact logic shipped in the app (MathCoachSolver). Use it to
 * validate each sim: every round, check the coach's answer + highlight + "why" are correct.
 */
(function () {
  if (window.__coachIv) clearInterval(window.__coachIv);
  var UNITS = { thousand: 1e3, lakh: 1e5, lakhs: 1e5, crore: 1e7, crores: 1e7, million: 1e6, billion: 1e9 };
  var nums = function (t) { return (t.match(/\d[\d,]*/g) || []).map(function (s) { return +s.replace(/,/g, ''); }); };
  var vals = function (t) {
    var re = /(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?/gi, m, o = [];
    while ((m = re.exec(t))) {
      if (!m[1]) continue;
      var b = parseFloat(m[1].replace(/,/g, '')); if (isNaN(b)) continue;
      var u = (m[2] || '').toLowerCase();
      o.push(Math.round(b * (u ? (UNITS[u] || UNITS[u.replace(/s$/, '')] || 1) : 1)));
    }
    return o;
  };
  var fmtIN = function (n) {
    var s = '' + Math.abs(n); if (s.length <= 3) return s;
    var h = s.slice(0, -3), tl = s.slice(-3), g = '', c = 0;
    for (var i = h.length - 1; i >= 0; i--) { g += h[i]; c++; if (c % 2 === 0 && i) g += ','; }
    return g.split('').reverse().join('') + ',' + tl;
  };
  var fmtINTL = function (n) { return ('' + n).replace(/\B(?=(\d{3})+(?!\d))/g, ','); };
  var placeName = function (p) {
    return ({ 10: 'ten', 100: 'hundred', 1000: 'thousand', 100000: 'lakh', 1000000: 'ten-lakh', 10000000: 'crore' }[p]) || fmtIN(p);
  };

  function solve(prompt, opts) {
    var t = prompt.toLowerCase(), L = opts.map(function (o) { return o.label; });
    // COMPARE
    if (opts.length && opts.every(function (o) { return ['<', '=', '>'].indexOf(o.label.trim()) >= 0; })) {
      var v = vals(prompt), a = v[0], b = v[1], sign = a < b ? '<' : a > b ? '>' : '=';
      var da = ('' + a).length, db = ('' + b).length;
      return { kind: 'compare', ans: sign, why: fmtIN(a) + ' ' + sign + ' ' + fmtIN(b) + ' — ' + (da !== db ? fmtIN(a > b ? a : b) + ' has more digits' : 'same length, compare from the left') };
    }
    // RATIO
    if (opts.length && opts.every(function (o) { return /^\d+(\.\d+)?\s*x$/i.test(o.label.trim()); })) {
      var vv = vals(prompt), r = vv[0] / vv[1];
      var best = opts.reduce(function (m, o) { return Math.abs(parseFloat(o.label) - r) < Math.abs(parseFloat(m.label) - r) ? o : m; });
      return { kind: 'ratio', ans: best.label, why: fmtIN(vv[0]) + ' ÷ ' + fmtIN(vv[1]) + ' ≈ ' + r.toFixed(1) + ' → ' + best.label };
    }
    // SPEED
    if (L.some(function (l) { return /km\/day/i.test(l); })) {
      var d = nums(prompt)[0], day = nums(prompt)[1], need = d / day;
      var bs = opts.reduce(function (m, o) { return Math.abs(nums(o.label)[0] - need) < Math.abs(nums(m.label)[0] - need) ? o : m; });
      return { kind: 'speed', ans: bs.label, why: fmtIN(d) + ' ÷ ' + day + ' ≈ ' + Math.round(need) + ' km/day → ' + bs.label };
    }
    // COMMA (position-checked: Indian slot must be Indian grouping, International slot International)
    if (L.some(function (l) { return /indian|international/i.test(l); })) {
      var N = nums(prompt).sort(function (a, b) { return ('' + b).length - ('' + a).length; })[0];
      var IN = fmtIN(N), INTL = fmtINTL(N), re = /indian\s+([\d,]+)\s+international\s+([\d,]+)/i;
      var co = opts.find(function (o) { var m = re.exec(o.label); return m && m[1].trim() === IN && m[2].trim() === INTL; });
      return { kind: 'comma', ans: co && co.label, why: 'Indian ' + IN + ' (first 3, then every 2); International ' + INTL + ' (every 3)' };
    }
    // ROUND (place from the gap between options)
    if (/round|nearest/.test(t)) {
      var ov = opts.map(function (o) { return nums(o.label)[0]; }).filter(function (x) { return !isNaN(x); }).sort(function (a, b) { return a - b; });
      var gaps = []; for (var i = 1; i < ov.length; i++) gaps.push(ov[i] - ov[i - 1]);
      var place = Math.min.apply(null, gaps.filter(function (g) { return g > 0; }));
      var Nn = Math.max.apply(null, nums(prompt).filter(function (x) { return x !== place && ov.indexOf(x) < 0; }));
      var ans = Math.round(Nn / place) * place, after = Math.floor(Nn / (place / 10)) % 10;
      var cor = opts.find(function (o) { return nums(o.label)[0] === ans; });
      return { kind: 'round', ans: cor && cor.label, why: 'digit after the ' + placeName(place) + 's place is ' + after + ', so ' + fmtIN(Nn) + ' rounds ' + (after >= 5 ? 'up' : 'down') + ' to ' + fmtIN(ans) };
    }
    // PATTERN — try several rules, prefer whichever lands on one of the options
    if (/pattern|next product|next term/.test(t)) {
      var seq = nums(prompt).filter(function (x) { return x > 0; }), s = seq.length - 1;
      while (s > 0 && seq[s - 1] < seq[s]) s--; seq = seq.slice(s);
      var optV = opts.map(function (o) { return nums(o.label)[0]; });
      var d0 = seq[0], ratio = seq[1] / seq[0], diff = seq[1] - seq[0], cand = [];
      if (seq.every(function (x, i) { return i === 0 || x === seq[i - 1] * 10 + d0; })) cand.push([seq[seq.length - 1] * 10 + d0, 'each term ×10 +' + d0]);
      if (seq[0] !== 0 && seq.every(function (x, i) { return i === 0 || x === seq[i - 1] * ratio; })) cand.push([Math.round(seq[seq.length - 1] * ratio), 'each term ×' + ratio]);
      if (seq.every(function (x, i) { return i === 0 || x - seq[i - 1] === diff; })) cand.push([seq[seq.length - 1] + diff, 'each term +' + diff]);
      var hit = cand.find(function (c) { return optV.indexOf(c[0]) >= 0; }) || cand[0];
      if (!hit) return null;
      var next = hit[0], cp = opts.find(function (o) { return nums(o.label)[0] === next; });
      return { kind: 'pattern', ans: cp && cp.label, digits: ('' + next).length, why: hit[1] + ' → ' + next + ' (' + ('' + next).length + ' digits)' };
    }
    // BUILD (needs the live running total)
    var cur = (document.body.innerText.match(/current[:\s]*([\d,]+)/i) || [])[1];
    if (cur != null && /build|target/.test(t)) {
      var target = Math.max.apply(null, nums(prompt)), c = +cur.replace(/,/g, '');
      var btns = opts.map(function (o) { return +o.value; }).filter(function (v) { return !isNaN(v); }).sort(function (a, b) { return b - a; });
      var fit = btns.find(function (v) { return c + v <= target; });
      var cb = opts.find(function (o) { return +o.value === fit; });
      return { kind: 'build', ans: cb && cb.label, why: c > target ? 'over ' + fmtIN(target) + ' — reset' : 'add +' + fmtIN(fit || 0) + ' (' + fmtIN(c) + '/' + fmtIN(target) + ')' };
    }
    return null; // combinatorial / non-math → method hints only
  }

  function read() {
    var m = document.querySelector('.mission, .problem, .question, .prompt');
    if (!m) return null;
    // whole mission text — some sims show numbers outside <p>/.num
    var prompt = (m.innerText || m.textContent || '').replace(/\s+/g, ' ').trim();
    var els = [].slice.call(document.querySelectorAll('.opt, .choice, button[data-v]'));
    var seen = []; els = els.filter(function (o) { if (seen.indexOf(o) >= 0) return false; seen.push(o); return true; });
    var opts = els.map(function (o) { return { el: o, label: o.innerText.trim().replace(/\s+/g, ' '), value: o.getAttribute('data-v') }; }).filter(function (o) { return o.label; });
    return { prompt: prompt, opts: opts };
  }

  var bar = document.getElementById('__coachBar');
  if (!bar) {
    bar = document.createElement('div'); bar.id = '__coachBar';
    bar.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:2147483647;background:#0e1230;color:#fff;font:13px/1.5 system-ui,sans-serif;padding:10px 14px;box-shadow:0 -6px 24px rgba(0,0,0,.4);border-top:2px solid #5b6cff';
    document.body.appendChild(bar);
  }
  // The sim marks the selected option with a `sel`/`selected`/`active` class (and re-creates the
  // buttons on click, so click-tracking is unreliable) — detect selection from the class instead.
  function anySelected(opts) {
    return opts.some(function (o) {
      var cs = (o.el.className || '').toLowerCase().split(/[\s_-]+/);
      return cs.indexOf('sel') >= 0 || cs.indexOf('selected') >= 0 || cs.indexOf('active') >= 0 ||
        cs.indexOf('chosen') >= 0 || cs.indexOf('picked') >= 0 ||
        o.el.getAttribute('aria-pressed') === 'true' || o.el.getAttribute('aria-checked') === 'true';
    });
  }
  function submitBtn() {
    return document.querySelector('#checkBtn') ||
      [].slice.call(document.querySelectorAll('button')).find(function (b) { return /check|lock|submit/i.test(b.innerText || ''); });
  }
  function tapBtn() { return document.querySelector('#tapBtn'); }
  function tick() {
    var r = read();
    if (!r) { bar.textContent = '🧭 coach: no .mission found on this page'; return; }
    var s = solve(r.prompt, r.opts);
    r.opts.forEach(function (o) { o.el.style.outline = ''; });
    if (window.__gA) { window.__gA.style.outline = ''; } if (window.__gB) { window.__gB.style.outline = ''; }
    if (window.__gC) { window.__gC.style.outline = ''; }
    window.__gA = null; window.__gB = null; window.__gC = null;
    var isBuild = s && s.kind === 'build';
    var buildDone = isBuild && !s.ans;              // solveBuild returns no next move when current == target
    var calc = !r.opts.length && tapBtn();
    // RED = the answer / next move to tap
    var glow = '—', ansEl = null;
    if (s && s.ans) { var co = r.opts.find(function (o) { return o.label === s.ans; }); if (co) { ansEl = co.el; glow = s.ans; } }
    if (!ansEl && calc) { ansEl = tapBtn(); glow = tapBtn().innerText; }
    if (ansEl) { ansEl.style.outline = '3px solid #ff5b5b'; ansEl.style.outlineOffset = '2px'; window.__gA = ansEl; }
    // GREEN = the submit button — only when it's actually time to submit:
    //   build → when the total matches the target · MCQ → after an option has been picked · calc → always
    var sb = submitBtn();
    var showSubmit = sb && (isBuild ? buildDone : (calc ? true : anySelected(r.opts)));
    if (showSubmit) { sb.style.outline = '3px solid #2e9e6b'; sb.style.outlineOffset = '2px'; window.__gB = sb; }
    // BLUE = a text input the sim also needs (e.g. 1_5 asks for the digit count of the term)
    var typeTxt = '';
    if (s && s.kind === 'pattern' && s.digits) {
      var inp = document.querySelector('input[type=number], input.digit, .digit input, input:not([type=hidden])');
      if (inp) { inp.style.outline = '3px solid #5b8bff'; inp.style.outlineOffset = '2px'; window.__gC = inp; }
      typeTxt = ' &nbsp;<span style="color:#9db8ff">● type:</span> <b>' + s.digits + '</b> (digits)';
    }
    var nextTxt = sb ? (showSubmit ? ' &nbsp;<span style="color:#7be0a4">🟢 now tap:</span> <b>' + (sb.innerText || 'submit').trim() + '</b>'
      : ' &nbsp;<span style="opacity:.6">(pick answer, then ' + (sb.innerText || 'submit').trim() + ')</span>')
      : (r.opts.length ? ' &nbsp;<span style="opacity:.6">(scores on tap — no Check)</span>' : '');
    bar.innerHTML = '🧭 <b>' + (s ? s.kind.toUpperCase() : (tapBtn() ? 'CALC' : 'METHOD-ONLY')) + '</b> &nbsp; <span style="opacity:.7">' + r.prompt.slice(0, 66) + '</span>'
      + '<br><span style="color:#ff9b9b">● tap:</span> <b style="color:#ffd0d0">' + (buildDone ? '(done)' : glow) + '</b>' + typeTxt + nextTxt
      + (s ? '<br><span style="opacity:.85">' + s.why + '</span>' : '<br><span style="opacity:.6">method-only sim — coach teaches the strategy here</span>');
  }
  window.__coachIv = setInterval(tick, 500); tick();
  return 'Coach injected — plays each round. Re-run to reset.';
})();

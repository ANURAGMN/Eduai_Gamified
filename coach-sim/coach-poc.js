/*!
 * EduCoach POC — one-clock coach for a single simulation.
 * Target sim: math_1_8 "Number System Compare Pitstop" (Compare).
 *
 * This is the production-grade proof of the architecture proposed for going forward:
 *
 *   (1) CONTRACT   — every round is described by ONE object, `window.__eduRound`.
 *                    Here a small PUBLISHER builds it by reading the DOM. Later the SIM itself can
 *                    set `window.__eduRound` in ~3 lines when a round loads, and the publisher below
 *                    is deleted — the CONSUMER/RENDER code stays byte-for-byte identical.
 *
 *   (2) ONE CLOCK  — a single loop reads the round, solves it, and renders glow + text + voice
 *                    TOGETHER from that one object. There is no second timer, no separate voice
 *                    queue, no phase machine. Nothing can drift out of sync because there is only
 *                    one source and one tick.
 *
 *   (3) VOICE      — one short line for the CURRENT round only, spoken once, cancelled the instant
 *                    the guidance changes. The full text lives on screen; the voice is a brief echo.
 *
 * Robustness (production): every tick is wrapped in try/catch so the coach can NEVER break the sim;
 * it only ever writes `outline` styles on controls and manages its own bar element (never mutates
 * the sim's data/DOM); it re-applies the glow every tick so it survives the sim re-creating its
 * option buttons; re-injection is idempotent; and `EduCoach.stop()` cleans everything up.
 *
 * Load: paste into the console on the live sim, or wrap as a bookmarklet. `EduCoach.debug = true`
 * logs one line per round for field diagnosis.
 */
(function () {
  'use strict';

  var CFG = {
    intervalMs: 300,
    colors: { hint: '#ff9500', submit: '#2e9e6b', wrong: '#ff1e1e' },
    voice: true,
    voiceRate: 1.05,
    debug: false,
  };

  // ---------------------------------------------------------------------------------------------
  // number parsing (unit-aware): "30 thousand" -> 30000, "3 lakh" -> 300000, "3,87,69,957" -> n
  // ---------------------------------------------------------------------------------------------
  var UNITS = { thousand: 1e3, lakh: 1e5, lakhs: 1e5, crore: 1e7, crores: 1e7, million: 1e6, billion: 1e9 };
  function valuesIn(t) {
    var re = /(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?/gi, m, out = [];
    while ((m = re.exec(t))) {
      if (!m[1]) continue;
      var b = parseFloat(m[1].replace(/,/g, '')); if (isNaN(b)) continue;
      var u = (m[2] || '').toLowerCase();
      out.push(Math.round(b * (u ? (UNITS[u] || UNITS[u.replace(/s$/, '')] || 1) : 1)));
    }
    return out;
  }

  // ---------------------------------------------------------------------------------------------
  // (1) PUBLISHER — build the ONE round object from the DOM.
  //     Contract shape:
  //       { kind, prompt, values:[String], options:[{id,label,el,selected}], submitEl,
  //         round:String, feedback:String, phase:'answer'|'result' }
  //     Swap this out when the sim sets window.__eduRound itself.
  // ---------------------------------------------------------------------------------------------
  function publish() {
    var mission = document.querySelector('.mission, .problem, .question');
    if (!mission) return null;
    var numEls = [].slice.call(mission.querySelectorAll('.num'));
    var values = numEls.length ? numEls.map(function (n) { return (n.innerText || '').trim(); }) : null;
    var options = [].slice.call(document.querySelectorAll('button.choice, .opt, .choice, button[data-v]'))
      .map(function (o) {
        return {
          id: o.getAttribute('data-v') || (o.innerText || '').trim(),
          label: (o.innerText || '').trim(),
          el: o,
          selected: /(^|\s)(sel|selected|active|chosen|picked)(\s|$)/.test(o.className || ''),
        };
      });
    var submitEl = [].slice.call(document.querySelectorAll('button'))
      .filter(function (b) { return /check|lock|submit/i.test(b.innerText || ''); })[0] || null;
    var feedback = ((document.querySelector('.msg') || {}).innerText || '').trim();
    var phase = /correct|right|not\b|wrong|bigger|smaller|✓|✗/i.test(feedback) ? 'result' : 'answer';
    return {
      kind: 'compare',
      prompt: (mission.innerText || '').replace(/\s+/g, ' ').trim(),
      values: values,
      options: options,
      submitEl: submitEl,
      round: ((document.querySelector('.v') || {}).innerText || '').trim(),
      feedback: feedback,
      phase: phase,
    };
  }

  // ---------------------------------------------------------------------------------------------
  // (2) CONSUMER (pure) — round -> { answerId, line }. One brain. Extend by adding `kind` cases.
  // ---------------------------------------------------------------------------------------------
  function solve(r) {
    if (r.kind === 'compare' && r.values && r.values.length >= 2) {
      var v = valuesIn(r.prompt), a = v[0], b = v[1];
      if (a == null || b == null) return { answerId: null, line: '' };
      var ans = a < b ? '<' : a > b ? '>' : '=';
      var line = ans === '='
        ? 'They are equal — tap the "=" sign.'
        : (a < b ? r.values[1] : r.values[0]) + ' is bigger — tap the "' + ans + '" sign.';
      return { answerId: ans, line: line };
    }
    return { answerId: null, line: '' };
  }

  // ---------------------------------------------------------------------------------------------
  // (3) RENDER — glow + text + voice, all from the same round, on the same tick.
  // ---------------------------------------------------------------------------------------------
  var glowed = [], bar = null, lastVoice = '', lastRoundKey = '';

  function clearGlow() {
    for (var i = 0; i < glowed.length; i++) { try { glowed[i].style.outline = ''; } catch (e) {} }
    glowed = [];
  }
  function glow(el, color) {
    if (!el) return;
    el.style.outline = '3px solid ' + color;
    el.style.outlineOffset = '2px';
    el.style.borderRadius = '8px';
    glowed.push(el);
  }
  function setBar(text) {
    if (!bar) {
      bar = document.createElement('div');
      bar.id = '__eduCoachBar';
      bar.style.cssText = 'position:fixed;left:8px;right:8px;bottom:8px;z-index:2147483647;' +
        'background:#0e1230;color:#fff;font:600 15px/1.35 system-ui,-apple-system,sans-serif;' +
        'padding:12px 16px;border-radius:12px;box-shadow:0 -6px 24px rgba(0,0,0,.45)';
      document.body.appendChild(bar);
    }
    bar.textContent = text;
  }
  var picked = null;
  function pickVoice() {
    if (picked) return picked;
    try {
      var vs = window.speechSynthesis.getVoices() || [];
      picked = vs.filter(function (v) { return /en[-_]?IN/i.test(v.lang); })[0] ||
        vs.filter(function (v) { return /^en/i.test(v.lang); })[0] || null;
    } catch (e) {}
    return picked;
  }
  function speak(text) {
    if (!CFG.voice || !text) return;
    try {
      window.speechSynthesis.cancel();
      var u = new SpeechSynthesisUtterance(text);
      u.rate = CFG.voiceRate;
      var v = pickVoice(); if (v) u.voice = v;
      window.speechSynthesis.speak(u);
    } catch (e) {}
  }

  function tick() {
    var r;
    try { r = publish(); } catch (e) { return; }        // never let a read error break the sim
    if (!r || !r.values) { window.__eduRound = r || null; return; }
    window.__eduRound = r;                                // <-- the contract, made explicit
    var c;
    try { c = solve(r); } catch (e) { c = { answerId: null, line: '' }; }

    // Re-apply glow EVERY tick so it survives the sim re-creating its buttons.
    clearGlow();
    var isResult = r.phase === 'result';
    var anyPicked = r.options.some(function (o) { return o.selected; });
    if (!isResult && c.answerId) {
      var ansEl = r.options.filter(function (o) { return o.id === c.answerId; })[0];
      glow(ansEl && ansEl.el, CFG.colors.hint);
    }
    if (!isResult && anyPicked) glow(r.submitEl, CFG.colors.submit);

    // Text (full, live) + voice (short, once per round). Both from THIS round only.
    var submitName = r.submitEl ? (r.submitEl.innerText || 'Check').trim() : 'Check';
    var barText = isResult
      ? '🧭 ' + r.feedback
      : '🧭 ' + (c.line || 'Read both numbers, then pick a sign.') + (anyPicked ? '  ·  now tap "' + submitName + '"' : '');
    setBar(barText);

    // A "round" is identified by its values + round number. Voice speaks the current guidance once
    // per round, and (separately) the result line once when it appears — always cancelling the prior.
    var roundKey = (r.values || []).join('|') + '#' + r.round;
    if (roundKey !== lastRoundKey) { lastRoundKey = roundKey; lastVoice = ''; }  // new round → allow a fresh line
    var voiceLine = isResult ? r.feedback : c.line;
    if (voiceLine && voiceLine !== lastVoice) { lastVoice = voiceLine; speak(voiceLine); }

    if (CFG.debug) {
      try { console.log('[EduCoach]', r.round, JSON.stringify(r.values), '-> tap', c.answerId, '|', (isResult ? 'result:' + r.feedback : 'hint')); } catch (e) {}
    }
  }

  // ---------------------------------------------------------------------------------------------
  // lifecycle — idempotent start/stop
  // ---------------------------------------------------------------------------------------------
  function start() { stop(); tick(); CFG._iv = setInterval(tick, CFG.intervalMs); return 'EduCoach POC running'; }
  function stop() {
    if (CFG._iv) { clearInterval(CFG._iv); CFG._iv = null; }
    try { window.speechSynthesis.cancel(); } catch (e) {}
    clearGlow();
    if (bar && bar.parentNode) bar.parentNode.removeChild(bar); bar = null;
  }

  window.EduCoach = {
    start: start, stop: stop,
    get state() { return window.__eduRound || null; },
    set debug(v) { CFG.debug = !!v; }, get debug() { return CFG.debug; },
    set voice(v) { CFG.voice = !!v; if (!v) { try { window.speechSynthesis.cancel(); } catch (e) {} } }, get voice() { return CFG.voice; },
  };

  return start();
})();

package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

/**
 * Injected after simulation HTML loads to capture taps, inputs, and verdict feedback.
 */
object SimulationInteractionScript {
    /** Delay before the second narration (footer / action hints) is reported to Android. */
    const val FOOTER_TTS_DELAY_MS = 75_000L

    /**
     * Safe `--vh` seed for every sim, plus an aggressive shell rescue ONLY for pages that
     * collapse to a solid dark body when `#app` height goes to 0 (notably science_4_10).
     * Do NOT force paper background / fixed #app height on other sims — that breaks layouts.
     */
    val vhRescueScript: String = """
        (function(){
            if (window.__eduVhRescueBound) return;
            window.__eduVhRescueBound = true;
            function readH(){
                var vv = window.visualViewport && window.visualViewport.height;
                var ih = window.innerHeight;
                var ch = document.documentElement && document.documentElement.clientHeight;
                var sh = (window.screen && (window.screen.availHeight || window.screen.height)) || 0;
                var best = Math.max(vv || 0, ih || 0, ch || 0);
                if(best > 100) return best;
                if(sh > 200) return sh;
                return 720;
            }
            function wantsShellRescue(){
                // Aggressive paper + fixed #app height is science_4_10-only.
                // Other sims share #app and break if we restyle their shell.
                var href = String(location.href || '') + String(location.pathname || '');
                return /science_4_10/i.test(href);
            }
            function forceShell(h){
                var body = document.body;
                var app = document.getElementById('app');
                if(body){
                    body.style.setProperty('background', '#EDEFEA', 'important');
                    body.style.setProperty('min-height', h + 'px', 'important');
                    body.style.setProperty('height', 'auto', 'important');
                }
                if(app){
                    app.style.setProperty('display', 'flex', 'important');
                    app.style.setProperty('visibility', 'visible', 'important');
                    app.style.setProperty('opacity', '1', 'important');
                    app.style.setProperty('background', '#EDEFEA', 'important');
                    app.style.setProperty('min-height', h + 'px', 'important');
                    app.style.setProperty('height', h + 'px', 'important');
                    app.style.setProperty('width', '100%', 'important');
                    app.style.setProperty('max-width', '100%', 'important');
                }
            }
            function apply(){
                var h = readH();
                document.documentElement.style.setProperty('--vh', (h * 0.01) + 'px');
                var shell = wantsShellRescue();
                if (shell) forceShell(h);
                try {
                    if(window.AndroidBridge && typeof window.AndroidBridge.reportSimulationIntro === 'function'){
                        window.AndroidBridge.reportSimulationIntro(
                            'eduFixVh h=' + Math.round(h) + ' shell=' + shell + ' app=' + !!document.getElementById('app')
                        );
                    }
                } catch(e) {}
            }
            apply();
            setTimeout(apply, 50);
            setTimeout(apply, 200);
            setTimeout(apply, 600);
            setTimeout(apply, 1200);
            window.addEventListener('resize', apply);
            window.addEventListener('orientationchange', function(){ setTimeout(apply, 50); setTimeout(apply, 300); });
            if(window.visualViewport){ window.visualViewport.addEventListener('resize', apply); }
        })();
    """.trimIndent()

    val injectionScript: String = """
        (function() {
            $vhRescueScript

            function sendEvent(name) {
                if (window.AndroidBridge && name && name.trim().length > 0) {
                    window.AndroidBridge.logButtonClick(name.trim());
                }
            }

            function isClickable(el) {
                try {
                    return window.getComputedStyle(el).cursor === 'pointer';
                } catch(e) {
                    return false;
                }
            }

            var lastLoggedText = '';
            var lastLoggedTime = 0;

            function sendButtonEvent(text) {
                var now = Date.now();
                if (text === lastLoggedText && (now - lastLoggedTime) < 500) return;
                lastLoggedText = text;
                lastLoggedTime = now;
                sendEvent(text);
            }

            function findClickableRoot(el) {
                var explicit = el.closest(
                    '[onclick], button, [role="button"], a, label, summary, ' +
                    'input[type="button"], input[type="submit"]'
                );
                if (explicit) return explicit;

                var candidate = el;
                for (var i = 0; i < 6; i++) {
                    if (!candidate || candidate === document.body) break;
                    if (candidate.getAttribute && candidate.getAttribute('onclick')) return candidate;
                    if (isClickable(candidate)) return candidate;
                    candidate = candidate.parentElement;
                }
                return null;
            }

            function resolveClickLabel(el) {
                var aria = el.getAttribute('aria-label') ||
                    el.getAttribute('data-track-label') ||
                    el.title;
                if (aria && aria.trim()) return aria.trim();

                var namedChild = el.querySelector(
                    '[class*="name"], [class*="label"], [class*="title"], [data-track-label]'
                );
                if (namedChild) {
                    var namedText = (namedChild.innerText ||
                        namedChild.getAttribute('data-track-label') || '').trim();
                    if (namedText && namedText.length <= 60) return namedText;
                }

                var text = (el.innerText || '').trim();
                if (text) {
                    var lines = text.split('\n');
                    for (var i = 0; i < lines.length; i++) {
                        var line = lines[i].trim();
                        if (line && line.length <= 60) return line;
                    }
                }

                if (el.id) return el.id.replace(/-/g, ' ');

                var onclick = el.getAttribute('onclick');
                if (onclick) {
                    var fn = onclick.match(/([a-zA-Z_${'$'}][\w${'$'}]*)\s*\(/);
                    if (fn) {
                        return fn[1]
                            .replace(/([a-z])([A-Z])/g, '${'$'}1 ${'$'}2')
                            .replace(/_/g, ' ')
                            .trim();
                    }
                }

                return 'interaction';
            }

            function logClickFromTarget(target) {
                var root = findClickableRoot(target);
                if (!root) return;
                sendButtonEvent(resolveClickLabel(root));
            }

            document.addEventListener('click', function(event) {
                logClickFromTarget(event.target);
            }, true);

            document.addEventListener('change', function(event) {
                var el = event.target;
                if (el.tagName === 'INPUT') {
                    var label = el.placeholder || el.getAttribute('aria-label') || el.name || el.id || 'Input';
                    if (el.type === 'range') {
                        sendEvent("Slider [" + label + "] set to: " + el.value);
                    } else {
                        sendEvent("Entered [" + label + "]: " + el.value);
                    }
                }
            }, true);

            var WRONG_PHRASE_RE = /\b(not correct|not yet|not quite|try again|not right|check again|Current sides|look again|that's not|wrong answer|needs work|need to|keep trying|lowest form|correct count|correct answer is|correct fix|not target|not reached|Chain incomplete|Target was|Correct form|Not exact|Closest is|Unsafe choice|Off target|Target is|Correct value)\b/i;
            var WRONG_WORD_RE   = /\b(wrong|incorrect|Expected|oops|mismatch|mistake|error|nope|revisit|rethink|need)\b/i;

            var CORRECT_PHRASE_RE = /\b(well done|great job|right answer|you got it|that's correct|good job|spot on|nicely done|well played|correct classification|Proof locked|Target built|Safe dispatch chosen|Grid unlocked)\b/i;
            var CORRECT_WORD_RE   = /\b(correct|correctly|great|success|excellent|bullseye|simplest|perfect|perfectly|bravo|amazing|awesome|fantastic|superb|brilliant|nailed|congrats|congratulations|solved|yay|well|done|achieved)\b/i;

            // Wrong feedback often REVEALS the answer ("The correct comparison is >", "Correct
            // value is 7,000", "the right answer was ..."). Such text contains "correct"/"right" but
            // means the learner was wrong — catch it so it isn't misread as a correct verdict.
            var WRONG_REVEAL_RE = /\b(correct|right)\s+(answer|value|comparison|sign|option|choice|order|result|number|form|one|solution|way|factor|estimate|grouping)\s+(is|was|should|=|:)/i;

            function isWrong(text) {
                return WRONG_PHRASE_RE.test(text) || WRONG_WORD_RE.test(text) ||
                    WRONG_REVEAL_RE.test(text);
            }

            function isCorrectVerdict(text) {
                return CORRECT_PHRASE_RE.test(text) || CORRECT_WORD_RE.test(text);
            }

            function isGreenish(rgb) {
                var m = rgb.match(/\d+/g);
                if (!m || m.length < 3) return false;
                var r = +m[0], g = +m[1], b = +m[2];
                return g > 80 && g > r * 1.3 && g > b * 1.2;
            }

            function isReddish(rgb) {
                var m = rgb.match(/\d+/g);
                if (!m || m.length < 3) return false;
                var r = +m[0], g = +m[1], b = +m[2];
                return r > 80 && r > g * 1.3 && r > b * 1.3;
            }

            var verdictTimer = null;

            function scheduleVerdict(isCorrectResult) {
                if (verdictTimer !== null) clearTimeout(verdictTimer);
                verdictTimer = setTimeout(function() {
                    verdictTimer = null;
                    if (window.AndroidBridge) {
                        window.AndroidBridge.logVerdict(isCorrectResult);
                    }
                }, 300);
            }

            function evaluateElement(el) {
                if (el.nodeType !== 1) return false;
                var text = el.innerText || el.textContent || '';
                if (text.length === 0) return false;

                var bg = '';
                try { bg = window.getComputedStyle(el).backgroundColor || ''; } catch(e) {}

                var green = isGreenish(bg);
                var red   = isReddish(bg);

                if (red) {
                    if (isWrong(text)) { scheduleVerdict(false); return true; }
                    return false;
                }
                if (green) {
                    if (isCorrectVerdict(text)) { scheduleVerdict(true); return true; }
                    return false;
                }

                if (isWrong(text))          { scheduleVerdict(false); return true; }
                if (isCorrectVerdict(text)) { scheduleVerdict(true);  return true; }
                return false;
            }

            function isVisible(el) {
                try {
                    var style = window.getComputedStyle(el);
                    return style.display !== 'none' &&
                           style.visibility !== 'hidden' &&
                           style.opacity !== '0';
                } catch(e) {
                    return false;
                }
            }

            function checkNewNode(node) {
                if (node.nodeType !== 1) return;
                if (!isVisible(node)) return;
                setTimeout(function() {
                    if (evaluateElement(node)) return;
                    var children = node.children;
                    for (var k = 0; k < children.length; k++) {
                        if (evaluateElement(children[k])) return;
                    }
                }, 0);
            }

            var observer = new MutationObserver(function(mutations) {
                for (var i = 0; i < mutations.length; i++) {
                    var mutation = mutations[i];
                    if (mutation.type === 'childList') {
                        var added = mutation.addedNodes;
                        for (var j = 0; j < added.length; j++) {
                            checkNewNode(added[j]);
                        }
                    }
                    if (mutation.type === 'attributes') {
                        checkNewNode(mutation.target);
                    }
                }
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['style', 'class']
            });

            function countTrackableElements() {
                var seen = new Set();
                var count = 0;

                function consider(el) {
                    if (!el || el === document.body || !isVisible(el)) return;
                    var label = resolveClickLabel(el);
                    var key = (el.tagName || '') + '|' + (el.id || label);
                    if (seen.has(key)) return;
                    seen.add(key);
                    count += 1;
                }

                var explicit = document.querySelectorAll(
                    '[onclick], button, [role="button"], a, label, summary, ' +
                    'input[type="button"], input[type="submit"], input[type="range"], ' +
                    '[data-track-label]'
                );
                for (var i = 0; i < explicit.length; i++) {
                    consider(explicit[i]);
                }

                var cursorNodes = document.querySelectorAll('*');
                for (var j = 0; j < cursorNodes.length; j++) {
                    var node = cursorNodes[j];
                    if (node.nodeType !== 1) continue;
                    if (isClickable(node)) consider(node);
                }

                return count;
            }

            function reportInteractionBudget() {
                if (!window.AndroidBridge ||
                    typeof window.AndroidBridge.reportInteractionBudget !== 'function') {
                    return;
                }
                var budget = countTrackableElements();
                window.AndroidBridge.reportInteractionBudget(Math.max(1, budget));
            }

            if (window.AndroidBridge && typeof window.AndroidBridge.onTrackingReady === 'function') {
                window.AndroidBridge.onTrackingReady();
            }
            reportInteractionBudget();

            function cleanText(value) {
                return (value || '').replace(/\s+/g, ' ').trim();
            }

            function joinParts(parts) {
                return parts.map(cleanText).filter(function(p) { return p; }).join('. ');
            }

            var SIM_ROOT_SELECTOR =
                '.sim-card, #sim, .simulation, .interactive, .game-area, .play-area, [data-interactive]';

            function isSimRoot(el) {
                if (!el || el.nodeType !== 1) return false;
                try {
                    return el.matches(SIM_ROOT_SELECTOR) || el.id === 'sim';
                } catch (e) {
                    return false;
                }
            }

            function blockText(el) {
                if (!el) return '';
                var heading = el.querySelector('h1, h2, h3, .title, .concept-title');
                var body = el.querySelector('p, .lede, .subtitle, .desc, .concept-text');
                return joinParts([
                    heading ? (heading.innerText || heading.textContent) : '',
                    body ? (body.innerText || body.textContent) : '',
                ]);
            }

            function extractKeyActionHints(simRoot) {
                if (!simRoot) return '';
                var hints = [];
                var seen = {};

                function addHint(value) {
                    var t = cleanText(value);
                    if (!t || t.length < 2 || seen[t]) return;
                    seen[t] = true;
                    hints.push(t);
                }

                var hintSelectors = [
                    '.static-hint', '.instruction', '.instruction-overlay',
                    '.control-label', '.prep-step .text', '[class*="hint"]', '#instruction'
                ];
                hintSelectors.forEach(function(sel) {
                    simRoot.querySelectorAll(sel).forEach(function(el) {
                        if (!isVisible(el)) return;
                        if (el.classList && el.classList.contains('hidden')) return;
                        addHint(el.innerText || el.textContent);
                    });
                });

                simRoot.querySelectorAll(
                    'button.test-btn, button.primary, .visual button, .controls button, button'
                ).forEach(function(btn) {
                    if (!isVisible(btn)) return;
                    if (hints.length >= 4) return;
                    var label = cleanText(btn.innerText || btn.textContent);
                    if (label && label.length <= 60) addHint(label);
                });

                return joinParts(hints);
            }

            /**
             * Top-of-page copy + key action hints inside the sim UI (on load).
             */
            function extractSimulationIntroText() {
                var parts = [];
                var root = document.querySelector('#app, main, [role="main"]') || document.body;
                var simRoot = null;

                if (root) {
                    var children = root.children;
                    for (var i = 0; i < children.length; i++) {
                        var child = children[i];
                        if (isSimRoot(child)) {
                            simRoot = child;
                            break;
                        }
                        var tag = (child.tagName || '').toLowerCase();
                        if (child.classList.contains('header') || tag === 'header' ||
                            child.classList.contains('hero') || child.classList.contains('intro')) {
                            var headerText = blockText(child);
                            if (headerText) parts.push(headerText);
                        } else if (child.classList.contains('concept-card')) {
                            var conceptText = blockText(child);
                            if (conceptText) parts.push(conceptText);
                        } else if (child.classList.contains('section-title') ||
                                   child.classList.contains('subtitle') ||
                                   child.classList.contains('lede')) {
                            addPlainText(parts, child);
                        }
                    }
                }

                if (!simRoot) {
                    simRoot = document.querySelector(SIM_ROOT_SELECTOR + ', #sim');
                }
                var actionHints = extractKeyActionHints(simRoot);
                if (actionHints) parts.push(actionHints);

                var joined = joinParts(parts);
                if (joined) return joined;

                var header = document.querySelector('.header, header, .hero, .intro');
                if (header) {
                    var headerOnly = blockText(header);
                    if (headerOnly) parts.push(headerOnly);
                }
                joined = joinParts(parts);
                if (joined) return joined;

                var docTitle = cleanText(document.title);
                if (docTitle && !/interactive|simulation|loading/i.test(docTitle)) {
                    return docTitle;
                }
                return '';
            }

            function addPlainText(parts, el) {
                var t = cleanText(el.innerText || el.textContent);
                if (t) parts.push(t);
            }

            /** Text blocks below the interactive sim — legend, insights, takeaways. */
            function extractSimulationFooterText() {
                var parts = [];
                var root = document.querySelector('#app, main, [role="main"]') || document.body;
                var passedSim = false;

                if (root) {
                    var children = root.children;
                    for (var i = 0; i < children.length; i++) {
                        var child = children[i];
                        if (!passedSim) {
                            if (isSimRoot(child)) passedSim = true;
                            continue;
                        }
                        if (!isVisible(child)) continue;
                        var text = cleanText(child.innerText || child.textContent);
                        if (text && text.length > 8) parts.push(text);
                    }
                }

                if (parts.length === 0) {
                    ['.legend', '.insight-box', '.real-world', '.takeaway', '.takeaway-text'].forEach(function(sel) {
                        document.querySelectorAll(sel).forEach(function(el) {
                            if (!isVisible(el)) return;
                            addPlainText(parts, el);
                        });
                    });
                }

                return joinParts(parts);
            }

            function reportSimulationIntroOnce(text) {
                if (!text || !window.AndroidBridge) return;
                if (typeof window.AndroidBridge.reportSimulationIntro === 'function') {
                    window.AndroidBridge.reportSimulationIntro(text);
                } else if (typeof window.AndroidBridge.reportKeyConcept === 'function') {
                    window.AndroidBridge.reportKeyConcept(text);
                }
            }

            function reportSimulationFooterOnce(text) {
                if (!text || !window.AndroidBridge) return;
                if (typeof window.AndroidBridge.reportSimulationFooter === 'function') {
                    window.AndroidBridge.reportSimulationFooter(text);
                }
            }

            var introReported = false;
            function tryReportSimulationIntro(attempt) {
                if (introReported) return;
                var text = extractSimulationIntroText();
                if (text && text.length > 5) {
                    introReported = true;
                    reportSimulationIntroOnce(text);
                    return;
                }
                if (attempt < 12) {
                    setTimeout(function() { tryReportSimulationIntro(attempt + 1); }, attempt < 3 ? 300 : 600);
                }
            }

            var footerReported = false;
            function tryReportSimulationFooter() {
                if (footerReported) return;
                var text = extractSimulationFooterText();
                if (!text) return;
                footerReported = true;
                reportSimulationFooterOnce(text);
            }

            tryReportSimulationIntro(0);
            // Capture the description early; Android decides when to read it aloud (~1.25 min).
            setTimeout(tryReportSimulationFooter, 2500);
            setTimeout(tryReportSimulationFooter, 6000);

            // ---------------- Guided coach ----------------
            // Harvest the interactive controls, stamp each with data-edu-step, report the
            // ordered structure to Android, expose a highlighter, and forward taps so the
            // coach can auto-advance. Fully generic — no per-simulation authoring.
            (function() {
                // Highlight is drawn as a positioned OVERLAY (not a CSS class on the target) so it
                // works on SVG / canvas / any element — CSS outline & box-shadow don't render on SVG.
                var hlStyle = document.createElement('style');
                hlStyle.textContent =
                    '@keyframes eduHlPulse{' +
                    '0%{box-shadow:0 0 0 0 rgba(255,30,30,0.9),0 0 12px 3px rgba(255,45,45,0.65);}' +
                    '50%{box-shadow:0 0 0 12px rgba(255,30,30,0),0 0 34px 14px rgba(255,45,45,0.95);}' +
                    '100%{box-shadow:0 0 0 0 rgba(255,30,30,0.9),0 0 12px 3px rgba(255,45,45,0.65);}}' +
                    // Amber "hint" pulse for proactive guidance (next move / suggested option) so it
                    // doesn't read as an error/wrong state; red is reserved for the missed answer.
                    '@keyframes eduHlPulseHint{' +
                    '0%{box-shadow:0 0 0 0 rgba(255,150,0,0.9),0 0 12px 3px rgba(255,170,0,0.6);}' +
                    '50%{box-shadow:0 0 0 12px rgba(255,150,0,0),0 0 34px 14px rgba(255,180,0,0.9);}' +
                    '100%{box-shadow:0 0 0 0 rgba(255,150,0,0.9),0 0 12px 3px rgba(255,170,0,0.6);}}' +
                    '#__edu_hl_overlay{position:fixed;pointer-events:none;z-index:2147483647;' +
                    'border:4px solid #ff1e1e;border-radius:10px;box-sizing:border-box;' +
                    'animation:eduHlPulse 0.85s ease-in-out infinite;}';
                document.head.appendChild(hlStyle);

                var eduCurrentEl = null;

                function eduOverlayEl() {
                    var o = document.getElementById('__edu_hl_overlay');
                    if (!o) {
                        o = document.createElement('div');
                        o.id = '__edu_hl_overlay';
                        o.style.display = 'none';
                        document.body.appendChild(o);
                    }
                    return o;
                }

                function positionEduOverlay() {
                    var o = document.getElementById('__edu_hl_overlay');
                    if (!o) return;
                    if (!eduCurrentEl) { o.style.display = 'none'; return; }
                    var r;
                    try { r = eduCurrentEl.getBoundingClientRect(); } catch (e) { return; }
                    if (!r || (r.width === 0 && r.height === 0)) { o.style.display = 'none'; return; }
                    var pad = 4;
                    o.style.display = 'block';
                    o.style.left = (r.left - pad) + 'px';
                    o.style.top = (r.top - pad) + 'px';
                    o.style.width = (r.width + pad * 2) + 'px';
                    o.style.height = (r.height + pad * 2) + 'px';
                }

                window.__eduHighlight = function(index, kind) {
                    if (index === null || index === undefined || index < 0) {
                        eduCurrentEl = null;
                        var oh = document.getElementById('__edu_hl_overlay');
                        if (oh) oh.style.display = 'none';
                        return;
                    }
                    var el = document.querySelector('[data-edu-step="' + index + '"]');
                    if (!el) {
                        // Stamps may have been wiped by a re-render — refresh and retry.
                        eduHarvest();
                        el = document.querySelector('[data-edu-step="' + index + '"]');
                    }
                    var sameAsBefore = (el && el === eduCurrentEl);
                    eduCurrentEl = el || null;
                    var ov = eduOverlayEl();
                    // Colour by intent: amber = a proactive hint (next move / suggestion); red = the
                    // answer the learner missed (reteach). Prevents the hint looking like an error.
                    if (kind === 'hint') {
                        ov.style.borderColor = '#ff9500';
                        ov.style.animationName = 'eduHlPulseHint';
                    } else {
                        ov.style.borderColor = '#ff1e1e';
                        ov.style.animationName = 'eduHlPulse';
                    }
                    if (!el) { positionEduOverlay(); return; }
                    // CRITICAL: never scroll a button that's already visible — a smooth re-centre on
                    // every re-highlight moves the button out from under the learner's finger (they
                    // reported "can't tap the glowing +1,000"). Only scroll when the target is truly
                    // off-screen, and do it instantly so nothing shifts mid-tap.
                    if (!sameAsBefore) {
                        var r = el.getBoundingClientRect();
                        var vh = window.innerHeight || document.documentElement.clientHeight;
                        var vw = window.innerWidth || document.documentElement.clientWidth;
                        var offscreen = (r.bottom <= 0) || (r.top >= vh) || (r.right <= 0) || (r.left >= vw);
                        if (offscreen) {
                            try { el.scrollIntoView({ block: 'center', inline: 'nearest' }); } catch (e) {}
                        }
                    }
                    positionEduOverlay();
                    setTimeout(positionEduOverlay, 250);
                };

                window.addEventListener('scroll', positionEduOverlay, true);
                window.addEventListener('resize', positionEduOverlay, true);

                function eduControlType(el) {
                    var tag = (el.tagName || '').toLowerCase();
                    if (tag === 'input') return (el.getAttribute('type') || 'text').toLowerCase();
                    if (tag === 'textarea') return 'text';
                    if (el.isContentEditable) return 'text';
                    var role = (el.getAttribute('role') || '').toLowerCase();
                    if (role === 'slider') return 'range';
                    if (role === 'textbox') return 'text';
                    return '';
                }

                function eduIsInput(type) {
                    return type === 'range' || type === 'text' || type === 'number' ||
                        type === 'tel' || type === 'search' || type === 'email';
                }

                function eduInputLabel(el, type) {
                    var lbl = '';
                    if (el.id) {
                        var forLbl = document.querySelector('label[for="' + el.id + '"]');
                        if (forLbl) lbl = (forLbl.innerText || '').trim();
                    }
                    if (!lbl && el.closest) {
                        var wrap = el.closest('label');
                        if (wrap) lbl = (wrap.innerText || '').trim();
                    }
                    if (!lbl) {
                        lbl = (el.getAttribute('aria-label') || el.getAttribute('title') ||
                            el.getAttribute('placeholder') || el.getAttribute('name') || '').trim();
                    }
                    if (!lbl && el.id) lbl = el.id.replace(/[-_]/g, ' ').trim();
                    if (!lbl) lbl = (type === 'range') ? 'slider' : 'input';
                    if (lbl.length > 60) lbl = lbl.substring(0, 60);
                    return lbl;
                }

                function eduHasLetters(s) { return /[a-zA-Z]/.test(s || ''); }

                // Many sims use emoji-only buttons (no text). Map the common ones to words so the
                // guide can name/target/narrate them ("lemon" instead of an unspeakable glyph).
                var EDU_EMOJI = {
                    '🍋':'lemon','🍶':'vinegar','🧴':'base solution','🥛':'milk','🧼':'soap',
                    '🥄':'baking soda','💧':'water','🚰':'tap water','🍬':'sugar solution','🧂':'salt',
                    '🌹':'rose','🌺':'flower','🟡':'turmeric','🍊':'orange','☕':'coffee','🫖':'tea',
                    '🥤':'soda','🍅':'tomato','🧅':'onion','🧪':'test tube','⚗️':'flask','🧫':'sample',
                    '🔋':'battery acid','🧹':'cleaner','🩸':'blood','🌊':'sea water','🥥':'coconut water'
                };

                // Derive a readable text label for an emoji/icon-only control: first a nearby text
                // caption (attr / sibling / child), then the emoji dictionary.
                function eduIconLabel(el) {
                    var cands = [];
                    if (el.getAttribute) {
                        cands.push(el.getAttribute('aria-label'));
                        cands.push(el.getAttribute('title'));
                        cands.push(el.getAttribute('data-name'));
                        cands.push(el.getAttribute('data-solution'));
                        cands.push(el.getAttribute('data-label'));
                    }
                    var ns = el.nextElementSibling; if (ns) cands.push(ns.textContent);
                    var ps = el.previousElementSibling; if (ps) cands.push(ps.textContent);
                    for (var i = 0; i < el.children.length; i++) cands.push(el.children[i].textContent);
                    for (var j = 0; j < cands.length; j++) {
                        var c = (cands[j] || '').replace(/\s+/g, ' ').trim();
                        if (c && eduHasLetters(c) && c.length <= 40) return c;
                    }
                    var txt = el.textContent || '';
                    for (var k in EDU_EMOJI) { if (txt.indexOf(k) >= 0) return EDU_EMOJI[k]; }
                    return null;
                }

                function eduHarvest() {
                    var nodes = [];
                    var seen = {};
                    var sel = '[onclick], button, [role="button"], a, label, summary, ' +
                        'input[type="button"], input[type="submit"], input[type="radio"], ' +
                        'input[type="checkbox"], input[type="range"], input[type="text"], ' +
                        'input[type="number"], input[type="tel"], input[type="search"], ' +
                        'input[type="email"], textarea, [contenteditable="true"], ' +
                        '[contenteditable=""], [role="slider"], [role="textbox"]';
                    function add(el) {
                        if (!el || !isVisible(el)) return;
                        // Skip anything nested inside an interactive control — the outer control is
                        // the real tap target. Its inner icon/label nodes (e.g. an emoji <span> in a
                        // <button>) must NOT get their own stamp, or a tap on the icon reports the
                        // wrong index and the guide won't advance (had to tap 2-3 times).
                        if (el.parentElement && el.parentElement.closest &&
                            el.parentElement.closest('button, [role="button"], a, [onclick], summary, [data-edu-step]')) {
                            return;
                        }
                        var tag = (el.tagName || '').toLowerCase();
                        if (tag === 'label') {
                            // A <label> annotates a control — it is NOT itself something to tap.
                            // Skip it when it points at (for=) or wraps a control, or when it isn't
                            // genuinely interactive on its own. Its text is still used to name the
                            // control it describes (see eduInputLabel). This stops the guide from
                            // highlighting things like the "String Length" caption above a slider.
                            if (el.htmlFor || el.getAttribute('for')) return;
                            if (el.querySelector('input, textarea, select, [contenteditable]')) return;
                            if (!el.getAttribute('onclick') && !isClickable(el)) return;
                        }
                        var type = eduControlType(el);
                        var input = eduIsInput(type);
                        // A wrapper/row that merely contains a form control isn't itself the
                        // target — let the inner input/slider be highlighted instead. Prevents the
                        // caption/container around a slider from getting its own highlight.
                        if (!input && el.querySelector &&
                            el.querySelector('input, textarea, select, [contenteditable], ' +
                                '[role="slider"], [role="textbox"]')) {
                            return;
                        }
                        var label = input ? eduInputLabel(el, type) : resolveClickLabel(el);
                        // Emoji/icon-only control (no letters) → resolve a readable text label.
                        if ((!label || label === 'interaction' || !eduHasLetters(label)) && !input) {
                            var textified = eduIconLabel(el);
                            if (textified) label = textified;
                        }
                        if (!label || (label === 'interaction' && !input) || !eduHasLetters(label)) return;
                        var key = (el.tagName || '') + '|' + (el.id || label);
                        if (seen[key]) return;
                        seen[key] = true;
                        var idx = nodes.length;
                        el.setAttribute('data-edu-step', String(idx));
                        nodes.push({ index: idx, label: label, tag: tag, type: type });
                    }
                    var list = document.querySelectorAll(sel);
                    for (var i = 0; i < list.length; i++) add(list[i]);
                    var all = document.querySelectorAll('*');
                    for (var j = 0; j < all.length; j++) {
                        var n = all[j];
                        if (n.nodeType === 1 && isClickable(n)) add(n);
                    }
                    return nodes;
                }

                function eduHarvestReadouts() {
                    var outs = [];
                    var seen = {};
                    var sel = '[aria-live], .result, .output, .readout, .reading, .display, ' +
                        '.score, .status, [class*="result"], [class*="output"], ' +
                        '[class*="reading"], [class*="display"], [class*="value"]';
                    var list = document.querySelectorAll(sel);
                    for (var i = 0; i < list.length && outs.length < 4; i++) {
                        var el = list[i];
                        if (!isVisible(el)) continue;
                        var label = '';
                        var lblEl = el.querySelector('.label, .name, [class*="label"], [class*="name"]');
                        if (lblEl) label = (lblEl.innerText || '').trim();
                        if (!label) label = (el.getAttribute('aria-label') || '').trim();
                        if (!label) {
                            var t = (el.innerText || '').trim();
                            var m = t.match(/^([A-Za-z][A-Za-z ]{1,28})[:=]/);
                            if (m) label = m[1].trim();
                        }
                        if (!label || label.length < 2 || label.length > 30) continue;
                        var lc = label.toLowerCase();
                        if (seen[lc]) continue;
                        seen[lc] = true;
                        outs.push(label);
                    }
                    return outs;
                }

                var eduReported = false;
                function reportGuide() {
                    if (eduReported) return;
                    var nodes = eduHarvest();
                    if (nodes.length === 0) return;
                    eduReported = true;
                    if (window.AndroidBridge &&
                        typeof window.AndroidBridge.reportGuideStructure === 'function') {
                        window.AndroidBridge.reportGuideStructure(
                            JSON.stringify({
                                title: document.title,
                                controls: nodes,
                                readouts: eduHarvestReadouts()
                            })
                        );
                    }
                }

                function eduStampedFrom(target) {
                    // Walk up from the real click target to the nearest stamped element. Works for
                    // SVG children and nested icon spans, unlike the clickable-root heuristic.
                    var el = target && target.closest ? target.closest('[data-edu-step]') : null;
                    if (el) return el;
                    var root = findClickableRoot(target);
                    if (root && root.getAttribute && root.getAttribute('data-edu-step') !== null) return root;
                    return null;
                }

                // ---- Maths coach: read the current on-screen problem so the coach can solve it ----
                function eduTxt(el){ return (el && (el.innerText || el.textContent) || '').trim().replace(/\s+/g,' '); }
                function reportMathProblem() {
                    if (!(window.AndroidBridge && typeof window.AndroidBridge.reportMathProblem === 'function')) return;
                    var mission = document.querySelector('.mission, .problem, .question, .prompt');
                    if (!mission) return;
                    // Use the WHOLE mission text — some sims show the number(s) in elements other
                    // than <p>/.num (e.g. population figure, the two compared values, the sequence).
                    var prompt = eduTxt(mission);
                    var optEls = [].slice.call(document.querySelectorAll('.opt, .choice, button[data-v]'));
                    // Speed sim (1_13) options like "500 km/day" aren't .opt/.choice/[data-v]. Add any
                    // control whose WHOLE text is exactly a km/day option (safe: won't match prose).
                    if (/km\/day|daily speed/i.test(prompt)) {
                        [].slice.call(document.querySelectorAll('button, [role="button"], .card, .option, li')).forEach(function(b){
                            if (/^[\d,]+\s*km\s*\/\s*day$/i.test(eduTxt(b)) && optEls.indexOf(b) < 0) optEls.push(b);
                        });
                    }
                    var eduSeenOpt = [];
                    optEls = optEls.filter(function(o){ if (eduSeenOpt.indexOf(o) >= 0) return false; eduSeenOpt.push(o); return true; });
                    var options = optEls.map(function(o, i){
                        // Numeric option buttons have no letters, so the main harvester skips them —
                        // stamp them here (high indices, no collision) so they can be highlighted.
                        var step = o.getAttribute('data-edu-step');
                        if (step === null) { step = String(9000 + i); o.setAttribute('data-edu-step', step); }
                        return { label: eduTxt(o), value: o.getAttribute('data-v'), step: step };
                    }).filter(function(o){ return o.label; });
                    var body = eduTxt(document.body);
                    var cm = body.match(/current[:\s]*([\d,]+)/i);
                    var payload = JSON.stringify({
                        prompt: prompt.slice(0, 220),
                        current: cm ? cm[1] : null,
                        options: options.slice(0, 12)
                    });
                    if (payload !== window.__eduLastMath) {   // only when the problem actually changes (new round)
                        window.__eduLastMath = payload;
                        window.AndroidBridge.reportMathProblem(payload);
                    }
                }

                document.addEventListener('click', function(event) {
                    var stamped = eduStampedFrom(event.target);
                    if (!stamped) {
                        // Controls may have re-rendered (stamps lost) — refresh and retry once.
                        eduHarvest();
                        stamped = eduStampedFrom(event.target);
                    }
                    if (stamped &&
                        window.AndroidBridge &&
                        typeof window.AndroidBridge.onGuideTap === 'function') {
                        window.AndroidBridge.onGuideTap(parseInt(stamped.getAttribute('data-edu-step'), 10));
                    }
                    // The problem may advance after a Check/option tap — re-read shortly after.
                    setTimeout(reportMathProblem, 250);
                    setTimeout(reportMathProblem, 700);
                }, true);

                // ---------- Continuous coach glow loop (the "pull" model) ----------
                // Owns the V3 practice highlight by re-reading the live DOM every 400ms, solving, and
                // outlining the right control(s) with CSS classes. Self-correcting: it re-derives from
                // ground truth each tick, so it can't freeze on a stale button or point at last round's
                // answer. Dormant until Kotlin calls __eduCoach.setActive(true).
                (function() {
                    var gcss = document.getElementById('__eduGlowCss');
                    if (!gcss) {
                        gcss = document.createElement('style'); gcss.id = '__eduGlowCss';
                        gcss.textContent =
                            '@keyframes eduGA{0%,100%{box-shadow:0 0 0 0 rgba(255,149,0,.9),0 0 10px 2px rgba(255,170,0,.6)}50%{box-shadow:0 0 0 9px rgba(255,149,0,0),0 0 28px 11px rgba(255,180,0,.95)}}' +
                            '@keyframes eduGR{0%,100%{box-shadow:0 0 0 0 rgba(255,30,30,.9),0 0 10px 2px rgba(255,60,60,.6)}50%{box-shadow:0 0 0 9px rgba(255,30,30,0),0 0 28px 11px rgba(255,60,60,.95)}}' +
                            '.edu-g-hint{outline:3px solid #ff9500 !important;outline-offset:2px !important;border-radius:8px;animation:eduGA .9s ease-in-out infinite !important}' +
                            '.edu-g-answer{outline:3px solid #ff1e1e !important;outline-offset:2px !important;border-radius:8px;animation:eduGR .9s ease-in-out infinite !important}' +
                            '.edu-g-submit{outline:3px solid #2e9e6b !important;outline-offset:2px !important;border-radius:8px}' +
                            '.edu-g-input{outline:3px solid #5b8bff !important;outline-offset:2px !important;border-radius:8px}';
                        document.head.appendChild(gcss);
                    }
                    var UNITS = { thousand:1e3, lakh:1e5, lakhs:1e5, crore:1e7, crores:1e7, million:1e6, billion:1e9 };
                    function nums(t){ return (t.match(/\d[\d,]*/g)||[]).map(function(s){return +s.replace(/,/g,'');}); }
                    function vals(t){ var re=/(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?/gi,m,o=[]; while((m=re.exec(t))){ if(!m[1])continue; var b=parseFloat(m[1].replace(/,/g,'')); if(isNaN(b))continue; var u=(m[2]||'').toLowerCase(); o.push(Math.round(b*(u?(UNITS[u]||UNITS[u.replace(/s$/,'')]||1):1))); } return o; }
                    function fmtIN(n){ var s=''+Math.abs(n); if(s.length<=3)return s; var h=s.slice(0,-3),tl=s.slice(-3),g='',c=0; for(var i=h.length-1;i>=0;i--){g+=h[i];c++;if(c%2===0&&i)g+=',';} return g.split('').reverse().join('')+','+tl; }
                    function fmtINTL(n){ return (''+n).replace(/\B(?=(\d{3})+(?!\d))/g,','); }
                    function basicCand(seq){ if(seq.length<2)return []; var d0=seq[0],ratio=seq[1]/seq[0],diff=seq[1]-seq[0],out=[]; if(seq.every(function(x,i){return i===0||x===seq[i-1]*10+d0;}))out.push(seq[seq.length-1]*10+d0); if(seq[0]!==0&&ratio===Math.floor(ratio)&&seq.every(function(x,i){return i===0||x===seq[i-1]*ratio;}))out.push(Math.round(seq[seq.length-1]*ratio)); if(seq.every(function(x,i){return i===0||x-seq[i-1]===diff;}))out.push(seq[seq.length-1]+diff); return out; }
                    function patCand(seq){ var out=basicCand(seq).slice(); var roots=seq.map(function(n){return Math.round(Math.sqrt(n));}); if(roots.length>=2 && roots.every(function(r,i){return r*r===seq[i];})){ var b=basicCand(roots); if(b.length)out.push(b[0]*b[0]); } return out; }
                    function readP(){ var m=document.querySelector('.mission, .problem, .question, .prompt'); if(!m) return null; var prompt=(m.innerText||m.textContent||'').replace(/\s+/g,' ').trim(); var els=[].slice.call(document.querySelectorAll('.opt, .choice, button[data-v]')); if(/km\/day|daily speed/i.test(prompt)){ [].slice.call(document.querySelectorAll('button, [role="button"], .card, .option, li')).forEach(function(b){ var tx=(b.innerText||'').trim().replace(/\s+/g,' '); if(/^[\d,]+\s*km\s*\/\s*day$/i.test(tx) && els.indexOf(b)<0) els.push(b); }); } var seen=[]; els=els.filter(function(o){ if(seen.indexOf(o)>=0)return false; seen.push(o); return true; }); var opts=els.map(function(o){ return { el:o, label:(o.innerText||'').trim().replace(/\s+/g,' '), value:o.getAttribute('data-v') }; }).filter(function(o){ return o.label; }); return { prompt:prompt, opts:opts }; }
                    function anySelected(opts){ return opts.some(function(o){ var cs=(o.el.className||'').toLowerCase().split(/[\s_-]+/); return cs.indexOf('sel')>=0||cs.indexOf('selected')>=0||cs.indexOf('active')>=0||cs.indexOf('chosen')>=0||cs.indexOf('picked')>=0||o.el.getAttribute('aria-pressed')==='true'||o.el.getAttribute('aria-checked')==='true'; }); }
                    function findBtn(re){ return [].slice.call(document.querySelectorAll('button, [role=button]')).filter(function(b){ return re.test((b.innerText||'').trim()); })[0]; }
                    function solveG(prompt, opts){
                        var t=prompt.toLowerCase(), L=opts.map(function(o){return o.label;});
                        if(opts.length && opts.every(function(o){return ['<','=','>'].indexOf(o.label.trim())>=0;})){ var v=vals(prompt),a=v[0],b=v[1]; return {kind:'mcq', ans:(a<b?'<':a>b?'>':'=')}; }
                        if(opts.length && opts.every(function(o){return /^\d+(\.\d+)?\s*x$/i.test(o.label.trim());})){ var vv=vals(prompt),r=vv[0]/vv[1]; var best=opts.reduce(function(m,o){return Math.abs(parseFloat(o.label)-r)<Math.abs(parseFloat(m.label)-r)?o:m;}); return {kind:'mcq', ans:best.label}; }
                        if(L.some(function(l){return /km\/day/i.test(l);})){ if(!opts.length)return null; var d=nums(prompt)[0],day=nums(prompt)[1],need=d/day; var bs=opts.reduce(function(m,o){return Math.abs(nums(o.label)[0]-need)<Math.abs(nums(m.label)[0]-need)?o:m;}); return {kind:'mcq', ans:bs.label}; }
                        if(L.some(function(l){return /indian|international/i.test(l);})){ var N=nums(prompt).sort(function(a,b){return (''+b).length-(''+a).length;})[0]; var IN=fmtIN(N),INTL=fmtINTL(N),cre=/indian\s+([\d,]+)\s+international\s+([\d,]+)/i; var co=opts.filter(function(o){var mm=cre.exec(o.label); return mm&&mm[1].trim()===IN&&mm[2].trim()===INTL;})[0]; return {kind:'mcq', ans:co&&co.label}; }
                        if(/round|nearest/.test(t)){ var ov=opts.map(function(o){return nums(o.label)[0];}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return a-b;}); var gaps=[]; for(var i=1;i<ov.length;i++)gaps.push(ov[i]-ov[i-1]); var place=Math.min.apply(null,gaps.filter(function(g){return g>0;})); var Nn=Math.max.apply(null,nums(prompt).filter(function(x){return x!==place&&ov.indexOf(x)<0;})); var ans=Math.round(Nn/place)*place; var cor=opts.filter(function(o){return nums(o.label)[0]===ans;})[0]; return {kind:'mcq', ans:cor&&cor.label}; }
                        if(/pattern|next product|next term/.test(t)){ var all=nums(prompt).filter(function(x){return x>0;}); var ovp=opts.map(function(o){return nums(o.label)[0];}); for(var st=0; st<all.length-1; st++){ var seq=all.slice(st); if(seq.length<2)break; var cands=patCand(seq); var hit=cands.filter(function(c){return ovp.indexOf(c)>=0;})[0]; if(hit!=null){ var cp=opts.filter(function(o){return nums(o.label)[0]===hit;})[0]; return {kind:'pattern', ans:cp&&cp.label, digits:(''+hit).length}; } } return {kind:'pattern', ans:null, digits:null}; }
                        // Build: read BOTH Target and Current from the .mission prompt only (not
                        // document.body, which can hold a stale total) and require an explicit Target
                        // (no Math.max fallback — it grabbed the Current value on an overshoot).
                        var cm=prompt.match(/current[:\s]*([\d,]+)/i), tm=prompt.match(/target[:\s]*([\d,]+)/i);
                        if(cm && tm && /build|target/.test(t)){ var target=+tm[1].replace(/,/g,''); var c=+cm[1].replace(/,/g,''); if(c===target)return {kind:'build', build:'lock'}; if(c>target)return {kind:'build', build:'reset'}; var btns=opts.map(function(o){return +o.value;}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return b-a;}); var fit=btns.filter(function(x){return c+x<=target;})[0]; var cb=opts.filter(function(o){return +o.value===fit;})[0]; return {kind:'build', build:'add', ans:cb&&cb.label}; }
                        return null;
                    }
                    var glowed=[];
                    function clearGlow(){
                        glowed.forEach(function(el){ if(el&&el.classList)el.classList.remove('edu-g-hint','edu-g-answer','edu-g-submit','edu-g-input'); });
                        glowed=[];
                        // Restore any digit-count placeholder hint we set (pattern sim).
                        if(window.__eduHintInput){ var hi=window.__eduHintInput; var orig=hi.getAttribute('data-edu-ph'); if(orig!==null){ hi.setAttribute('placeholder', orig); hi.removeAttribute('data-edu-ph'); } window.__eduHintInput=null; }
                    }
                    function addGlow(el,cls){ if(el&&el.classList){ el.classList.add(cls); glowed.push(el); } }
                    var active=false, reteach=false;
                    function tick(){
                        clearGlow(); if(!active) return;
                        var r=readP(); if(!r) return; var s=solveG(r.prompt, r.opts); if(!s) return;
                        var hintCls = reteach ? 'edu-g-answer' : 'edu-g-hint';
                        if(s.kind==='build'){
                            if(s.build==='lock'){ addGlow(findBtn(/check|lock|submit/i), 'edu-g-submit'); }
                            // Overshoot → Reset; but after a failed Lock the sim replaces Reset with a
                            // "Next Target" button, so fall back to that so the glow points at a live control.
                            else if(s.build==='reset'){ addGlow(findBtn(/reset/i) || findBtn(/next\s*target|next round|next/i), hintCls); }
                            else if(s.ans){ var bo=r.opts.filter(function(o){return o.label===s.ans;})[0]; if(bo)addGlow(bo.el, hintCls); }
                            return;
                        }
                        if(s.ans){ var co=r.opts.filter(function(o){return o.label===s.ans;})[0]; if(co)addGlow(co.el, hintCls); }
                        if(s.kind==='pattern' && s.digits){
                            var inp=document.querySelector('input[type=number], input.digit, .digit input, input:not([type=hidden])');
                            if(inp){
                                addGlow(inp, 'edu-g-input');
                                // The glow shows WHERE to type; also surface WHAT to type — kids were
                                // getting the digit count wrong even with the field highlighted. Put the
                                // count in the placeholder while the field is empty (restored in clearGlow).
                                if(inp.getAttribute('data-edu-ph')===null){ inp.setAttribute('data-edu-ph', inp.getAttribute('placeholder')||''); }
                                if(!inp.value){ inp.setAttribute('placeholder', 'Type '+s.digits+' — that many digits'); }
                                window.__eduHintInput=inp;
                            }
                        }
                        if(anySelected(r.opts)){ addGlow(findBtn(/check|lock|submit/i), 'edu-g-submit'); }
                    }
                    window.__eduCoach = {
                        setActive: function(v){ active=!!v; if(!active)clearGlow(); },
                        setReteach: function(v){ reteach=!!v; }
                    };
                    if (!window.__eduCoachIv) window.__eduCoachIv = setInterval(tick, 400);
                })();

                // ---------- V4 one-clock coach ----------
                // A single self-contained loop: reads each round, solves ALL chapter-1 math sims,
                // glows the right control (reusing the .edu-g-* classes), and pushes the CURRENT short
                // line to Kotlin — coachText (display) and coachSpeak (voice) — on the SAME tick, so
                // glow + text + voice can never drift. Dormant until __eduCoachV4.setActive(true).
                // Validated offline (compare/round/ratio/comma/pattern incl. squares/speed/build).
                (function(){
                    var UN={thousand:1e3,lakh:1e5,lakhs:1e5,crore:1e7,crores:1e7,million:1e6,billion:1e9};
                    function nums(t){return (t.match(/\d[\d,]*/g)||[]).map(function(s){return +s.replace(/,/g,'');});}
                    function vals(t){var re=/(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?/gi,m,o=[];while((m=re.exec(t))){if(!m[1])continue;var b=parseFloat(m[1].replace(/,/g,''));if(isNaN(b))continue;var u=(m[2]||'').toLowerCase();o.push(Math.round(b*(u?(UN[u]||UN[u.replace(/s$/,'')]||1):1)));}return o;}
                    function fmtIN(n){var s=''+Math.abs(n);if(s.length<=3)return s;var h=s.slice(0,-3),tl=s.slice(-3),g='',c=0;for(var i=h.length-1;i>=0;i--){g+=h[i];c++;if(c%2===0&&i)g+=',';}return g.split('').reverse().join('')+','+tl;}
                    function fmtINTL(n){return (''+n).replace(/\B(?=(\d{3})+(?!\d))/g,',');}
                    function bC(seq){if(seq.length<2)return [];var d0=seq[0],r=seq[1]/seq[0],df=seq[1]-seq[0],o=[];if(seq.every(function(x,i){return i===0||x===seq[i-1]*10+d0;}))o.push(seq[seq.length-1]*10+d0);if(seq[0]!==0&&r===Math.floor(r)&&seq.every(function(x,i){return i===0||x===seq[i-1]*r;}))o.push(Math.round(seq[seq.length-1]*r));if(seq.every(function(x,i){return i===0||x-seq[i-1]===df;}))o.push(seq[seq.length-1]+df);return o;}
                    function pC(seq){var o=bC(seq).slice();var rt=seq.map(function(n){return Math.round(Math.sqrt(n));});if(rt.length>=2&&rt.every(function(r,i){return r*r===seq[i];})){var b=bC(rt);if(b.length)o.push(b[0]*b[0]);}return o;}
                    function fB(re){return [].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(b){return re.test((b.innerText||'').trim());})[0]||null;}
                    function publish(){var mi=document.querySelector('.mission, .problem, .question');if(!mi)return null;var prompt=(mi.innerText||'').replace(/\s+/g,' ').trim();var els=[].slice.call(document.querySelectorAll('.opt,.choice,button[data-v]'));if(/km\/day|daily speed/i.test(prompt)){[].slice.call(document.querySelectorAll('button,[role=button],.card,.option')).forEach(function(b){if(/^[\d,]+\s*km\s*\/\s*day$/i.test((b.innerText||'').trim())&&els.indexOf(b)<0)els.push(b);});}var opts=els.map(function(o){return {id:o.getAttribute('data-v')||o.innerText.trim(),label:(o.innerText||'').trim().replace(/\s+/g,' '),value:o.getAttribute('data-v'),el:o,selected:/(^|\s)(sel|selected|active|chosen|picked)(\s|$)/.test(o.className||'')};}).filter(function(o){return o.label;});var fb=((document.querySelector('.msg,.result,.feedback')||{}).innerText||'').trim();return {prompt:prompt,opts:opts,submitEl:fB(/check|lock|submit/i),resetEl:fB(/reset/i),nextEl:fB(/next/i),feedback:fb,round:((document.querySelector('.v')||{}).innerText||'').trim(),phase:/correct|right|not\b|wrong|bigger|smaller|exact/i.test(fb)?'result':'answer'};}
                    function solve(r){var t=r.prompt.toLowerCase(),L=r.opts.map(function(o){return o.label;});if(r.opts.length&&r.opts.every(function(o){return ['<','=','>'].indexOf(o.label.trim())>=0;})){var v=vals(r.prompt),a=v[0],b=v[1],an=a<b?'<':a>b?'>':'=';return {glow:an,submitOnPick:1,line:an==='='?'They are equal - tap "=".':'Tap "'+an+'" - the first number is '+(a<b?'smaller':'larger')+'.'};}if(r.opts.length&&r.opts.every(function(o){return /^\d+(\.\d+)?\s*x$/i.test(o.label.trim());})){var vv=vals(r.prompt),rr=vv[0]/vv[1],bt=r.opts.reduce(function(m,o){return Math.abs(parseFloat(o.label)-rr)<Math.abs(parseFloat(m.label)-rr)?o:m;});return {glow:bt.label,submitOnPick:1,line:'About '+rr.toFixed(1)+' times - tap "'+bt.label+'".'};}if(L.some(function(l){return /km\/day/i.test(l);})&&r.opts.length){var d=nums(r.prompt)[0],dy=nums(r.prompt)[1],nd=d/dy,bs=r.opts.reduce(function(m,o){return Math.abs(nums(o.label)[0]-nd)<Math.abs(nums(m.label)[0]-nd)?o:m;});return {glow:bs.label,submitOnPick:1,line:'Need about '+Math.round(nd)+' km/day - tap "'+bs.label+'".'};}if(L.some(function(l){return /indian|international/i.test(l);})){var N=nums(r.prompt).sort(function(a,b){return (''+b).length-(''+a).length;})[0],IN=fmtIN(N),IT=fmtINTL(N),cre=/indian\s+([\d,]+)\s+international\s+([\d,]+)/i,co=r.opts.filter(function(o){var mm=cre.exec(o.label);return mm&&mm[1].trim()===IN&&mm[2].trim()===IT;})[0];return {glow:co&&co.label,line:'Indian '+IN+', International '+IT+' - pick that card.'};}if(/round|nearest/.test(t)){var ov=r.opts.map(function(o){return nums(o.label)[0];}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return a-b;}),gp=[];for(var i=1;i<ov.length;i++)gp.push(ov[i]-ov[i-1]);var pl=Math.min.apply(null,gp.filter(function(g){return g>0;})),Nn=Math.max.apply(null,nums(r.prompt).filter(function(x){return x!==pl&&ov.indexOf(x)<0;})),ans=Math.round(Nn/pl)*pl,cor=r.opts.filter(function(o){return nums(o.label)[0]===ans;})[0];return {glow:cor&&cor.label,submitOnPick:1,line:'Rounds to '+fmtIN(ans)+' - tap it.'};}if(/pattern|next product|next term/.test(t)){var all=nums(r.prompt).filter(function(x){return x>0;}),ovp=r.opts.map(function(o){return nums(o.label)[0];});for(var st=0;st<all.length-1;st++){var sq=all.slice(st);if(sq.length<2)break;var hit=pC(sq).filter(function(c){return ovp.indexOf(c)>=0;})[0];if(hit!=null){var cp=r.opts.filter(function(o){return nums(o.label)[0]===hit;})[0];return {glow:cp&&cp.label,digits:(''+hit).length,submitOnPick:1,line:'Next is '+hit+' ('+(''+hit).length+' digits).'};}}return {line:'Find the rule between the terms.'};}// CALC (1_11): one allowed button, press to reach target.
                    var am=r.prompt.match(/allowed button[:\s]*\+?([\d,]+)/i);if(am){var step=+am[1].replace(/,/g,''),tmc=r.prompt.match(/target[:\s]*([\d,]+)/i),tg1=tmc?+tmc[1].replace(/,/g,''):null,bigEl=document.querySelector('.mission .big, .big'),cur=bigEl?+(((bigEl.innerText||'').match(/[\d,]+/)||['0'])[0].replace(/,/g,'')):0,tapEl=document.querySelector('#tapBtn'),chkEl=document.querySelector('#checkBtn');if(tg1!=null){if(cur>=tg1)return {submitGlowEl:chkEl,line:'Reached '+fmtIN(tg1)+' - tap Check.',vkey:'ck'};var rem=Math.max(0,Math.round((tg1-cur)/step));return {glowEl:tapEl,line:'Tap +'+fmtIN(step)+' - '+rem+' more ('+fmtIN(cur)+' of '+fmtIN(tg1)+').',voice:'Keep tapping '+step+'.',vkey:'calc'};}}
                    // MAXIMIZE A+B (1_1): greedy - biggest digits to the biggest place values.
                    if(/maximize\s+a\s*\+\s*b/i.test(r.prompt)){var am2=r.prompt.match(/a\s*\((\d+)\s*digit/i),bm2=r.prompt.match(/b\s*\((\d+)\s*digit/i),ml=am2?+am2[1]:5,nl=bm2?+bm2[1]:4,db=[].slice.call(document.querySelectorAll('button.digit')),used=db.filter(function(b){return b.disabled||/used/.test(b.className);}).length,W=[],i,j;for(i=0;i<ml;i++)W.push(Math.pow(10,ml-1-i));for(j=0;j<nl;j++)W.push(Math.pow(10,nl-1-j));var ord=W.map(function(w,ix){return {ix:ix,w:w};}).sort(function(x,y){return y.w-x.w||x.ix-y.ix;}),cards=db.map(function(b){return +b.innerText.trim();}).sort(function(a,b){return b-a;}),seq=[];ord.forEach(function(o,rk){seq[o.ix]=cards[rk];});var nd=seq[used],nb=db.filter(function(b){return +b.innerText.trim()===nd&&!b.disabled;})[0];if(nb)return {glowEl:nb,line:'Biggest digits in the highest places - tap '+nd+'.',voice:'Tap '+nd+'.',vkey:'m'+used};return {line:'Place the biggest digits in the highest place values.',vkey:'m'};}
                    // VAULT (1_3): rule-aware strategy hint (rules vary; a correct hint beats a risky glow).
                    if(/vault rule|using digits/i.test(r.prompt)){var big=/largest|biggest|greatest/i.test(r.prompt),sm=/smallest|least/i.test(r.prompt),end='';if(/multiple of 5/i.test(r.prompt))end=' It must end in 0 or 5.';else if(/even/i.test(r.prompt))end=' It must end in an even digit.';else if(/odd/i.test(r.prompt))end=' It must end in an odd digit.';var ordr=big?'Place the biggest digits first, 9 down.':sm?'Place the smallest digits first, no leading 0.':'Arrange the digits to fit the rule.';return {line:ordr+end,voice:ordr,vkey:'vault'};}
                    // TARGET DASH (1_7): expression search is fragile - give a concrete strategy hint.
                    if(/build expression/i.test(r.prompt)){var tv=r.prompt.match(/target value[:\s]*([\d,]+)/i),tt=tv?tv[1]:'the target';return {line:'Reach '+tt+': start with the biggest cards and x or -, then nudge with the small cards.',voice:'Start with the biggest cards, then adjust.',vkey:'expr'};}
                    var cm=r.prompt.match(/current[:\s]*([\d,]+)/i),tm=r.prompt.match(/target[:\s]*([\d,]+)/i);if(cm&&tm){var tg=+tm[1].replace(/,/g,''),c=+cm[1].replace(/,/g,'');if(c===tg)return {ctrl:'submit',line:'You matched the target - tap Lock Build.',vkey:'lock'};if(c>tg)return {ctrl:'reset',line:'Over the target - tap Reset.',voice:'Over the target - tap Reset.',vkey:'reset'};var bt2=r.opts.map(function(o){return +o.value;}).filter(function(x){return !isNaN(x);}).sort(function(a,b){return b-a;}),fit=bt2.filter(function(x){return c+x<=tg;})[0],cb=r.opts.filter(function(o){return +o.value===fit;})[0];return {glow:cb&&cb.label,line:'Add +'+fmtIN(fit)+' ('+fmtIN(c)+' of '+fmtIN(tg)+').',voice:'Add '+fit+'.',vkey:'a'+fit};}return {line:''};}
                    var G4=[],lastText='',lastVoice='',lastRK='',hi4=null,active4=false;
                    function bridge(){return window.AndroidBridge;}
                    function pushText(t){if(t===lastText)return;lastText=t;try{if(bridge()&&bridge().coachText)bridge().coachText(t);}catch(e){}}
                    function clearGlow4(){G4.forEach(function(e){try{e.classList.remove('edu-g-hint','edu-g-submit','edu-g-input');}catch(x){}});G4=[];if(hi4){var o=hi4.getAttribute('data-ph');if(o!==null){hi4.setAttribute('placeholder',o);hi4.removeAttribute('data-ph');}hi4=null;}}
                    function gl4(el,cls){if(el&&el.classList){el.classList.add(cls);G4.push(el);}}
                    // ---- Science chapter 1 (Acids/Bases): knowledge-based, no .mission. Read the
                    // selected substance, classify it (from its own label if present, else a known table),
                    // and glow the test action. Safe: on any unrecognised sim it returns null (no coach).
                    var ACIDBASE={lemon:'acid','lemon juice':'acid',vinegar:'acid',curd:'acid',yogurt:'acid',tamarind:'acid','tamarind water':'acid',orange:'acid','orange juice':'acid',tomato:'acid','tomato juice':'acid','citric acid':'acid','hydrochloric acid':'acid','apple':'acid','grapes':'acid',soap:'base','soap solution':'base','baking soda':'base','washing soda':'base','lime water':'base',ammonia:'base','sodium hydroxide':'base','milk of magnesia':'base','antacid':'base',water:'neutral','tap water':'neutral','distilled water':'neutral',sugar:'neutral','sugar solution':'neutral',salt:'neutral','salt solution':'neutral','common salt':'neutral',milk:'neutral'};
                    function sciSel(){return document.querySelector('.solution-btn.active, .substance.active, .material.active, .option.active, .card.active, [class*=solution][class*=active]');}
                    function sciAction(){return [].slice.call(document.querySelectorAll('button,[role=button]')).filter(function(b){var t=(b.innerText||'').toLowerCase();return /dip|mix|test|smell|add|pour|rub|check|react/.test(t)&&t.length<30;})[0];}
                    function sciName(el){return ((el&&el.innerText)||'').replace(/\(.*?\)/g,'').replace(/[^a-zA-Z\s]/g,'').replace(/\s+/g,' ').trim();}
                    function sciClass(el){var label=((el&&el.innerText)||'').replace(/\s+/g,' ').trim();var m=label.match(/\((acid|base|basic|neutral)\)/i);return m?m[1].toLowerCase():ACIDBASE[sciName(el).toLowerCase()];}
                    function sciSubs(){var els=[].slice.call(document.querySelectorAll('.solution-btn, .substance, .material, .sample'));var seen=[],out=[];els.forEach(function(e){var n=sciName(e);if(n&&seen.indexOf(n)<0){seen.push(n);out.push(e);}});return out;}
                    // Walk the learner through testing EVERY substance (like v1), not just the selected one.
                    function scienceSolve(){
                        var subs=sciSubs();if(!subs.length)return null;
                        var key=subs.map(sciName).sort().join('|');if(window.__eduSciKey!==key){window.__eduSciKey=key;window.__eduSciDone={};}
                        if(!window.__eduSciBound){window.__eduSciBound=true;document.addEventListener('click',function(ev){var b=ev.target&&ev.target.closest?ev.target.closest('button,[role=button]'):null;if(!b)return;if(/dip|mix|test|smell|add|pour|rub|react/i.test(b.innerText||'')){var s=sciSel();if(s){var nm=sciName(s);if(nm){window.__eduSciDone=window.__eduSciDone||{};window.__eduSciDone[nm]=true;}}}},true);}
                        var done=window.__eduSciDone||{},act=sciAction(),an=act?((act.innerText||'test').replace(/[^\w\s]/g,'').trim()):'test';
                        var next=subs.filter(function(b){return !done[sciName(b)];})[0];
                        if(!next)return {line:'You tested them all! Acids turn litmus red, bases turn it blue, neutral stays the same.',voice:'You have tested them all.',vkey:'sci-done:'+key};
                        var nm=sciName(next),active=sciSel();
                        if(active&&sciName(active)===nm){var cls=sciClass(active),norm=cls?(cls.indexOf('acid')>=0?'an acid':cls.indexOf('neutral')>=0?'neutral':'a base'):null;if(norm)return {glowEl:act,line:nm+' is '+norm+' - tap '+an+' to test it.',voice:nm+' is '+norm+'.',vkey:'sci:'+nm};return {glowEl:act,line:'Tap '+an+' to test '+nm+' and watch the result.',voice:'',vkey:'sci:'+nm};}
                        return {glowEl:next,line:'Next, test '+nm+' - tap it.',voice:'Now test '+nm+'.',vkey:'sci-sel:'+nm};
                    }
                    // ---- Math chapter 2 (Arithmetic Expressions): read the active expression, count
                    // its terms (top-level +/-), and evaluate it (product/quotient bind tighter). Safe:
                    // a tiny evaluator, no eval(); returns null on anything that isn't a clean expression.
                    function findActiveExpr(){var els=[].slice.call(document.querySelectorAll('[class*=active], .selected, .current, .expr, .display, .problem-display'));for(var i=0;i<els.length;i++){var tx=(els[i].innerText||'').replace(/\s+/g,' ').trim();if(tx.length<40&&/\d/.test(tx)&&/[×÷*\/+−\-]/.test(tx)&&/^[\d\s×÷*\/+−\-.]+$/.test(tx))return tx;}return null;}
                    function evalExpr(s){var t=s.replace(/\s+/g,'');var parts=t.split(/(?=[+\-])/);var total=0,cnt=0;parts.forEach(function(p){if(!p)return;cnt++;var sg=1;if(p[0]==='+')p=p.slice(1);else if(p[0]==='-'){sg=-1;p=p.slice(1);}var fs=p.split(/([*\/])/);var v=parseFloat(fs[0]);for(var k=1;k<fs.length;k+=2){var op=fs[k],n=parseFloat(fs[k+1]);if(op==='*')v*=n;else v/=n;}if(!isNaN(v))total+=sg*v;});return {terms:cnt,value:total};}
                    function exprSolve(){var a=findActiveExpr();if(!a)return null;var nrm=a.replace(/−/g,'-').replace(/×/g,'*').replace(/÷/g,'/').replace(/[^\d+\-*\/.]/g,'');if(!/[+\-*\/]/.test(nrm)||!/\d/.test(nrm))return null;var r=evalExpr(nrm);if(isNaN(r.value))return null;var ts=r.terms+' term'+(r.terms===1?'':'s');return {line:a+' -> '+ts+', value = '+r.value+'.',voice:ts+', value '+r.value+'.',vkey:'expr:'+a};}
                    function scienceTick(){var sp;try{sp=exprSolve()||scienceSolve();}catch(e){sp=null;}if(!sp){pushText('');return;}if(sp.glowEl)gl4(sp.glowEl,'edu-g-hint');pushText(sp.line);var vk=sp.vkey||sp.line;if(vk!==lastVoice){lastVoice=vk;try{if(sp.voice&&bridge()&&bridge().coachSpeak)bridge().coachSpeak(sp.voice);}catch(e){}}}
                    // ---- NATIVE PUBLISH: a sim can set window.__eduRound = {line, voice?, glow?,
                    // submit?, input?, inputHint?, key?} whenever its state changes. If present, the
                    // coach renders it DIRECTLY (no scraping) — one clock, correct by construction.
                    // glow/submit/input are CSS selectors (or elements). Set window.__eduRound=null to clear.
                    function pubEl(sel){if(!sel)return null;if(sel.nodeType)return sel;try{return document.querySelector(sel);}catch(e){return null;}}
                    function renderPublished(pub){if(pub.glow)gl4(pubEl(pub.glow),pub.glowKind==='answer'?'edu-g-answer':'edu-g-hint');if(pub.submit)gl4(pubEl(pub.submit),'edu-g-submit');if(pub.input){var ie=pubEl(pub.input);if(ie){gl4(ie,'edu-g-input');if(pub.inputHint!=null&&!ie.value){if(ie.getAttribute('data-ph')===null)ie.setAttribute('data-ph',ie.getAttribute('placeholder')||'');ie.setAttribute('placeholder',''+pub.inputHint);hi4=ie;}}}var line=pub.line||'';pushText(line);var vk=pub.key||pub.voice||line,voice=(pub.voice!=null?pub.voice:line);if(vk&&vk!==lastVoice){lastVoice=vk;try{if(voice&&bridge()&&bridge().coachSpeak)bridge().coachSpeak(voice);}catch(e){}}}
                    function tick4(){if(window.__eduV5){clearGlow4();return;}clearGlow4();if(!active4)return;var pub=window.__eduRound;if(pub&&typeof pub==='object'&&(pub.line||pub.glow)){try{renderPublished(pub);}catch(e){}return;}var r;try{r=publish();}catch(e){return;}if(!r){scienceTick();return;}window.__eduScraped=r;var p;try{p=solve(r);}catch(e){p={line:''};}var isR=r.phase==='result';var picked=r.opts.some(function(o){return o.selected;});if(!isR){if(p.glow){var ge=r.opts.filter(function(o){return o.label===p.glow;})[0];gl4(ge&&ge.el,'edu-g-hint');}if(p.glowEl)gl4(p.glowEl,'edu-g-hint');if(p.submitGlowEl)gl4(p.submitGlowEl,'edu-g-submit');if(p.ctrl==='submit')gl4(r.submitEl,'edu-g-submit');if(p.ctrl==='reset')gl4(r.resetEl||r.nextEl,'edu-g-hint');if(p.submitOnPick&&picked)gl4(r.submitEl,'edu-g-submit');if(p.digits){var inp=document.querySelector('input[type=number],input.digit,.digit input,input:not([type=hidden])');if(inp){gl4(inp,'edu-g-input');if(inp.getAttribute('data-ph')===null)inp.setAttribute('data-ph',inp.getAttribute('placeholder')||'');if(!inp.value)inp.setAttribute('placeholder','Type '+p.digits);hi4=inp;}}}var sn=r.submitEl?(r.submitEl.innerText||'Check').trim():'Check';var textLine=isR?r.feedback:((p.line||'')+((p.line&&p.submitOnPick&&picked&&r.submitEl)?('  -  now tap "'+sn+'"'):''));var rk=(r.prompt||'').replace(/current.*/i,'')+'#'+r.round;if(rk!==lastRK){lastRK=rk;lastVoice='';}pushText(textLine);
                        // VOICE dedups on a STABLE key (not the full line) so the build sim doesn't
                        // re-speak on every tap as the running total climbs — it speaks each move once.
                        var voiceText=isR?r.feedback:(p.voice||p.line);var vkey=isR?('r:'+r.feedback):(p.vkey||p.line);if(vkey&&vkey!==lastVoice){lastVoice=vkey;try{if(bridge()&&bridge().coachSpeak)bridge().coachSpeak(voiceText);}catch(e){}}}
                    // Honor a wanted-flag set by Kotlin BEFORE this IIFE ran (guide can unlock
                    // before onPageFinished injects). Without this, setActive(true) is a no-op and
                    // the loop stays dormant forever.
                    active4 = !!window.__eduCoachV4Wanted;
                    window.__eduCoachV4={setActive:function(v){window.__eduCoachV4Wanted=!!v;active4=!!v;if(!active4){clearGlow4();pushText('');}else{try{tick4();}catch(e){}}}};
                    if(!window.__eduCoachV4Iv)window.__eduCoachV4Iv=setInterval(tick4,300);
                    if(active4){try{tick4();}catch(e){}}
                })();

                // Retry a few times so async-rendered sims are captured.
                setTimeout(reportGuide, 400);
                setTimeout(reportGuide, 1200);
                setTimeout(reportGuide, 2500);
                setTimeout(reportMathProblem, 600);
                setTimeout(reportMathProblem, 1500);
                setTimeout(reportMathProblem, 2800);
            })();

        })();
    """.trimIndent()
}

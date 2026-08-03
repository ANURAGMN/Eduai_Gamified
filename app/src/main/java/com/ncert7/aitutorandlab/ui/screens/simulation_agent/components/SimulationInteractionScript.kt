package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

/**
 * Injected after simulation HTML loads to capture taps, inputs, and verdict feedback.
 */
object SimulationInteractionScript {
    /** Delay before the second narration (footer / action hints) is reported to Android. */
    const val FOOTER_TTS_DELAY_MS = 75_000L

    val injectionScript: String = """
        (function() {

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

            function isWrong(text) {
                return WRONG_PHRASE_RE.test(text) || WRONG_WORD_RE.test(text);
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

                window.__eduHighlight = function(index) {
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
                    eduCurrentEl = el || null;
                    eduOverlayEl();
                    if (!el) { positionEduOverlay(); return; }
                    try { el.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch (e) {}
                    positionEduOverlay();
                    // Reposition after the smooth-scroll settles.
                    setTimeout(positionEduOverlay, 250);
                    setTimeout(positionEduOverlay, 600);
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
                        if (!label || (label === 'interaction' && !input)) return;
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
                }, true);

                // Retry a few times so async-rendered sims are captured.
                setTimeout(reportGuide, 400);
                setTimeout(reportGuide, 1200);
                setTimeout(reportGuide, 2500);
            })();

        })();
    """.trimIndent()
}

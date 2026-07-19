package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

/**
 * Injected after simulation HTML loads to capture taps, inputs, and verdict feedback.
 */
object SimulationInteractionScript {
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

            function getButtonText(el) {
                var btn = el.closest('button, [role="button"], a');
                if (!btn) return '';
                return (btn.innerText || btn.getAttribute('aria-label') || btn.title || '').trim();
            }

            document.addEventListener('mousedown', function(event) {
                var text = getButtonText(event.target);
                if (text.length > 0) sendButtonEvent(text);
            }, true);

            document.addEventListener('touchstart', function(event) {
                var text = getButtonText(event.target);
                if (text.length > 0) sendButtonEvent(text);
            }, true);

            document.addEventListener('click', function(event) {
                var el = event.target;

                var btn = el.closest('button, [role="button"], a');
                if (btn) {
                    var text = (btn.innerText || btn.getAttribute('aria-label') || btn.title || '').trim();
                    if (text.length > 0) sendButtonEvent(text);
                    return;
                }

                var candidate = el;
                var found = false;
                for (var i = 0; i < 3; i++) {
                    if (!candidate || candidate === document.body) break;
                    if (isClickable(candidate) && candidate.children.length <= 1) {
                        found = true;
                        break;
                    }
                    candidate = candidate.parentElement;
                }
                if (found) {
                    var text = (candidate.innerText || '').trim();
                    if (text.length > 0 && text.length <= 60) {
                        sendEvent(text);
                    }
                }
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

        })();
    """.trimIndent()
}

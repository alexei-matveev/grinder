jQuery(function($) {

    function addChangeDetection(scope) {
        var changeables = $(".changeable", scope);

        if (!changeables.length) {
            return;
        }

        $("label", scope).each(function() {
            var l = this;

            if (l.htmlFor != '') {
                var e = $("#" + l.htmlFor, scope)[0];

                if (e) {
                    e.label = this;
                } else {
                    $("[name='" + l.htmlFor + "']", scope).each(function() {
                        this.label = l;
                    });
                }
            }
        });

        jQuery.fn.visible = function(show) {
            return this.css("visibility", show ? "visible" : "hidden");
        };

        var submit = $("#submit", scope);
        submit.visible(false);

        changeables.each(function() {
            if (this.type === "checkbox") {
                this.modified = function() {
                    return this.checked != this.defaultChecked;
                };
            } else {
                var original = this.value;
                this.modified = function() {
                    return original != this.value;
                };
            }

            $(this).change(function(e) {
                // This is wrong if multiple controls share the same label.
                $(e.target.label).toggleClass("changed", e.target.modified());

                submit.visible(changeables.filter(function(x) {
                    return this.modified();
                }).length);
            });
        });
    }

    function createPoller(e) {

        var tokens = {}; // key => token
        var listeners = {}; // key => {listeners}
        var xhr = null;

        var poller = {
            poll : function() {
                if (xhr != null) {
                    xhr.abort();
                    xhr = null;
                }

                if ($.isEmptyObject(tokens)) {
                    return;
                }

                var p = this;

                xhr = $.getJSON("/ui/poll", tokens);

                xhr.then(function(x) {
                    $.each(x, function(_k, v) {
                        if (tokens.hasOwnProperty(v.key)) {
                            $.each(listeners[v.key],
                                   function() { this(v.key, v.value); });

                            tokens[v.key] = v.next;
                        }
                        else {
                            console.warn("Ignoring value with unknown key", v);
                        }
                    });
                })
                .then(function() {
                    p.poll();
                });
            },

            subscribe : function(e, k, t, f) {
                var p = this;

                var unsubscribe = function() {
                    var l = listeners[k];

                    if (l) {
                        delete l[f];

                        if ($.isEmptyObject(l)) {
                            delete listeners[k];
                            delete tokens[k];
                            p.poll();
                        }
                    }
                };

                unsubscribe();

                $(document).bind("DOMNodeRemoved", function(r) {
                    if (r.target == e) {
                        unsubscribe();
                    }
                });

                var l = listeners[k] || { };
                l[f] = f;
                listeners[k] = l;

                if (!t || tokens[k] && tokens[k] != t) {
                    // No token supplied, or there is a listener registered
                    // for a different token. Request the current value.
                    tokens[k] = "-1";
                }
                else {
                    tokens[k] = t;
                }

                this.poll();
            },
        };

        return poller;
    };

    var poller = createPoller(document);

    function addLiveDataElements(scope) {

        $(".ld-display").each(function() {
            var t = $(this);

            poller.subscribe(this, t.data("ld-key"), t.data("ld-token"),
                function(k, v) {
                    t.html(v);
                });
        });

        $(".ld-animate").each(function() {
            var t = $(this);

            poller.subscribe(this, t.data("ld-key"), t.data("ld-token"),
                function(k, v) {
                    t
                    .stop()
                    .animate({opacity: 0.5},
                            "fast",
                            function() {
                                $(this)
                                    .html(v)
                                    .animate({opacity: 1}, "fast");
                            });
                });
         });

    }

    function addButtons(scope) {
        content = $("div#content");

        $(".grinder-button", scope).each(function() {
            if (this.id) {

                var buttonOptions;

                if (this.classList.contains("grinder-button-icon")) {
                    buttonOptions = {
                            icons: { primary: this.id }
                    };
                }
                else {
                    buttonOptions = {};
                }

                $(this).button(buttonOptions);

                if (this.classList.contains("replace-content")) {
                    $(this).click(function() {
                        $.get("/ui/content/" + this.id,
                           function(x) {
                                content.animate(
                                    {opacity: 0},
                                    "fast",
                                    function() {
                                        content.html(x);
                                        addDynamicBehaviour(content);
                                        content.animate({opacity: 1}, "fast");
                                    });
                           });
                        });
                }
                else {
                    $(this).click(function() {
                        $.post("/ui/action/" + this.id);
                    });
                }
            } else {
                $(this).button();
            };
        });
    }

    function cubismCharts(scope) {
        var cubismDiv = $("#cubism");

        if (!cubismDiv.length) {
            return;
        }

        var context = cubism.context()
                        .step(2000)
                        .size(cubismDiv.width());

        // Maybe there's a neater way to do this with d3?
        $(document).bind("DOMNodeRemoved", function(e) {
            if (e.target == cubismDiv) {
                context.stop();
            }
        });

        context.on("focus", function(i) {
            d3.selectAll(".value")
            .style("right",
                    i == null ? null : context.size() - i + "px");
        });

        // Should constrain D3 selection to scope, but how?
        d3.select("#cubism").selectAll(".axis")
            .data(["top", "bottom"])
            .enter().append("div")
            .attr("class", function(d) { return d + " axis"; })
            .each(function(d) {
                d3.select(this).call(context.axis().orient(d)); });

        d3.select("#cubism").append("div")
            .attr("class", "rule")
            .call(context.rule());

        const TESTS_STATISTIC = 0;
        const TOTAL_TEST = "Total";

        var selectedStatistic = TESTS_STATISTIC;

        // A function that updates the bound d3 data.
        var newData = function(metrics) {

            var selection = d3.select("#cubism").selectAll(".horizon");

            // Bind tests to nodes.
            var binding = selection
                .data(function() { return metrics; },
                      function(metric) { return metric.key; });

            // Handle new nodes.
            binding.enter().insert("div", ".bottom")
                .attr("class", "horizon")
                .call(context.horizon()
                        .format(d3.format(",.3r"))
                        .colors(["#225EA8",
                                 "#41B6C4",
                                 "#A1DAB4",
                                 "#FFFFCC",
                                 "#FECC5C",
                                 "#FD8D3C",
                                 "#F03B20",
                                 "#BD0026"]));

            binding.exit().remove();

            binding.sort(function(a, b) {
                if (a.test === TOTAL_TEST) {
                    return b.test === TOTAL_TEST ? 0 : 1;
                }
                else if (b.test === TOTAL_TEST) {
                    return -1;
                }

                return d3.ascending(a.test, b.test);
            });
        };


        // Separate out the newData() update, so we can push this subscription
        // up to document level.
        // Will need to move the cubism context too.
        var metrics = [];

        poller.subscribe(scope, "sample", undefined, function(k, v) {
            var existingByTest = {};

            $(metrics).each(function() {
                existingByTest[this.test] = this;
            });

            metrics = $.map(v.tests, function(t) {
                var metric =
                    existingByTest[t.test] ||
                    createMetric(t.test,
                                 t.description,
                                 selectedStatistic);

                metric.add(v.timestamp, t.statistics);

                return metric;
            });

            var totalMetric =
                existingByTest[TOTAL_TEST] ||
                createMetric(TOTAL_TEST, null, selectedStatistic);

            totalMetric.add(v.timestamp, v.totals);

            metrics.push(totalMetric);

            newData(metrics);
        });


        // Create a cubism metric for a given test and the selected statistic.
        //
        // The following are added to the metric:
        //   key              - a pair of the test number and statistic.
        //   test             - the test number, or TOTAL_TEST.
        //   withStatistic(s) - clones the metric for a new statistic s.
        //   add(t,s)         - adds a new sample s at timestamp t.
        function createMetric(test, description) {

            // We may want to replace this with a binary tree.
            // For now we just have an array in timestamp order.
            // Each element is an array pair of timestamp and statistic.
            // We assume that we're called with increasing timestamps.
            var stats = [];

            var average = function(ss, s) {
                // Cubism uses NaN to indicate "no value".
                var total =
                    ss.reduce(function(x, y) {
                        // If tests = 0, there is no value.
                        var v = y[TESTS_STATISTIC] ? y[s] : NaN;

                        if (isNaN(v)) { return x; }

                        if (isNaN(x)) { return v; }

                        return x + v;
                    }, NaN);

                return total / ss.length;
            };

            var metric_fn = function(statistic) {
                return function(start, stop, step, callback) {
                    var values = [];

                    start = +start; // Date -> timestamp.
                    var x, ss;

                    var previousBetween = function() {
                        var d = stats.length - 1;

                        return function(s, e) {
                            x = stats[d];

                            while (x && x[0] >= s) {
                                d -= 1;
                                if (x[0] < e) {
                                    return x[1];
                                }

                                x = stats[d];
                            }
                        };
                    }();

                    for (var i = +stop; i > start; i-= step) {
                        ss = [];

                        while (x = previousBetween(i - step, i)) {
                            ss.push(x);
                        }

                        values.unshift(average(ss, statistic));
                    }

                    callback(null, values);
                };
            };

            var d = test;

            if (description) {
                d = d + " [" + description + "]";
            }

            function createMetric(s) {
                var metric = context.metric(metric_fn(s), d);
                metric.key = [test, s];
                metric.test = test;
                metric.withStatistic = createMetric;

                metric.add = function(timestamp, sample) {
                    stats.push([timestamp, sample]);
                };

                return metric;
            };

            return createMetric(selectedStatistic);
        }

        $("select[name=chart-statistic]").val(selectedStatistic);

        $("select[name=chart-statistic]").change(
                function() {
                    selectedStatistic = this.value;

                    metrics = $.map(metrics, function(old) {
                        return old.withStatistic(selectedStatistic);
                    });

                    newData(metrics);
                });
    }

    function addDataPanels(scope) {
        var data_state = null;
        var process_threads = null;

        poller.subscribe(scope, "sample", undefined, function(k, v) {
            var s = v.status;
            var d = $("#data-summary");

            d.html(s.description);

            if (s.state != data_state) {
                if (s.state === "Stopped") {
                    d.parent().stop().animate({opacity: 0}, "slow");
                }
                else {
                    d.parent().stop().animate({opacity: 0.9}, "fast");
                }

                data_state = s.state;
            }
        });

        poller.subscribe(scope, "threads", undefined, function(k, v) {
            var p = $("#process-summary");

            p.html("<span>a:" + v.agents + " </span>" +
                   "<span>w:" + v.workers + " </span>" +
                   "<span>t:" + v.threads + " </span>");

            if (v.threads != process_threads) {
                if (v.threads === 0) {
                    p.parent().stop().animate({opacity: 0}, "slow");
                }
                else {
                    p.parent().stop().animate({opacity: 0.9}, "fast");
                }

                process_threads = v.threads;
            }
        });
    }

    function addDynamicBehaviour(scope) {
        addButtons(scope);
        addChangeDetection(scope);
        addLiveDataElements(scope);
        cubismCharts(scope);
    }

    addDataPanels(document);
    addDynamicBehaviour(document);
});

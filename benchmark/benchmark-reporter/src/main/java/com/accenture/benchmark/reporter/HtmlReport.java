/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.accenture.benchmark.reporter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a benchmark run into a single self-contained HTML document — inline CSS and inline SVG only, no
 * external stylesheets, scripts, fonts, or network calls — so the file opens identically anywhere (a
 * browser, a CI artifact viewer, an email attachment). Two charts per workload: a log-scaled latency
 * histogram and a percentile ("nines") plot that makes the tail visible.
 */
public final class HtmlReport {

    private static final String[] COLORS = {"#2563eb", "#db2777", "#059669", "#d97706"};

    private static final int W = 760;
    private static final int HT = 300;
    private static final String SVG_OPEN = String.format(Locale.US,
            "<svg viewBox=\"0 0 %d %d\" xmlns=\"http://www.w3.org/2000/svg\" class=\"svg\">", W, HT) + "\n";
    private static final String DIV_CLOSE = "</div>\n";

    private HtmlReport() {
    }

    private static void fmtln(StringBuilder g, String format, Object... args) {
        g.append(String.format(Locale.US, format, args)).append('\n');
    }

    public static String render(Map<String, String> env, List<WorkloadResult> results) {
        StringBuilder h = new StringBuilder(1 << 16);
        h.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Mercury benchmark report</title>\n")
                .append("<style>\n").append(CSS).append("\n</style>\n</head>\n<body>\n");

        h.append("<h1>Mercury benchmark report</h1>\n");
        h.append("<p class=\"sub\">End-to-end latency &amp; throughput in a single JVM. "
                + "<b>Normal operation</b> scenarios keep the arrival rate at or below consumer capacity, so the "
                + "ElasticQueue stays within its in-memory buffer; the <b>overload</b> scenario drives it into "
                + "the disk-backed back-pressure buffer. Together they show how the event system behaves both in "
                + "healthy steady state and under sustained overload.</p>\n");

        // environment
        h.append("<div class=\"card\"><h2>Environment</h2>\n<table class=\"env\">\n");
        for (Map.Entry<String, String> e : env.entrySet()) {
            h.append("<tr><th>").append(escape(e.getKey())).append("</th><td>")
                    .append(escape(e.getValue())).append("</td></tr>\n");
        }
        h.append("</table></div>\n");

        // workloads, grouped by category
        int i = 0;
        String lastCategory = "";
        for (WorkloadResult r : results) {
            if (!r.category().equals(lastCategory)) {
                h.append("<h2 class=\"category\">").append(escape(r.category())).append("</h2>\n");
                lastCategory = r.category();
            }
            String color = COLORS[i % COLORS.length];
            i++;
            Stats s = r.stats();
            h.append("<div class=\"card\">\n");
            h.append("<h2><span class=\"dot\" style=\"background:").append(color).append("\"></span>")
                    .append(escape(r.name())).append("</h2>\n");
            h.append("<p class=\"desc\">").append(escape(r.description())).append("</p>\n");

            // summary chips
            h.append("<div class=\"chips\">\n");
            chip(h, "throughput", num(r.throughput(), 0) + " ops/s");
            chip(h, "ok", num(s.count()));
            chip(h, "failures", num(r.failures()));
            chip(h, "elapsed", num(r.elapsedSec(), 2) + " s");
            for (Map.Entry<String, String> p : r.params().entrySet()) {
                chip(h, p.getKey(), p.getValue());
            }
            h.append(DIV_CLOSE);

            // stats table
            h.append("<table class=\"stats\">\n<thead><tr>")
                    .append("<th>min</th><th>mean</th><th>std dev</th><th>p50</th><th>p90</th>")
                    .append("<th>p99</th><th>p99.9</th><th>p99.99</th><th>max</th></tr></thead>\n<tbody><tr>");
            td(h, s.minMs());
            td(h, s.meanMs());
            td(h, s.stddevMs());
            td(h, s.p50());
            td(h, s.p90());
            td(h, s.p99());
            td(h, s.p999());
            td(h, s.p9999());
            td(h, s.maxMs());
            h.append("</tr></tbody></table>\n<p class=\"unit\">latencies in milliseconds</p>\n");

            // charts
            h.append("<div class=\"charts\">\n");
            h.append("<div class=\"chart\"><h3>Latency distribution</h3>\n")
                    .append(svgHistogram(s, color)).append(DIV_CLOSE);
            h.append("<div class=\"chart\"><h3>Latency by percentile</h3>\n")
                    .append(svgPercentiles(s, color)).append(DIV_CLOSE);
            h.append(DIV_CLOSE).append(DIV_CLOSE);
        }

        h.append("<p class=\"foot\">Generated by benchmark-reporter — a self-contained Mercury performance "
                + "harness. Re-run with different <code>-Dbench.*</code> / <code>-Delastic.queue.store</code> "
                + "settings to compare.</p>\n");
        h.append("</body>\n</html>\n");
        return h.toString();
    }

    // ---- SVG: log-scaled histogram of latency buckets ----
    private static String svgHistogram(Stats s, String color) {
        final double left = 44;
        final double right = 12;
        final double top = 14;
        final double bottom = 60;
        double plotW = W - left - right;
        double plotH = HT - top - bottom;
        List<Long> bins = s.binCounts();
        long max = 1;
        for (long c : bins) {
            if (c > max) {
                max = c;
            }
        }
        double logMax = Math.log10(max + 1.0);
        double slot = plotW / bins.size();
        double barW = slot * 0.78;

        StringBuilder g = new StringBuilder();
        g.append(SVG_OPEN);
        // baseline
        g.append(line(left, top + plotH, left + plotW, top + plotH, "#d1d5db"));
        for (int i = 0; i < bins.size(); i++) {
            double x = left + i * slot + (slot - barW) / 2;
            if (bins.get(i) > 0) {
                double bh = plotH * (Math.log10(bins.get(i) + 1.0) / logMax);
                double y = top + plotH - bh;
                fmtln(g,
                        "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\" rx=\"1\">"
                                + "<title>%s: %,d</title></rect>",
                        x, y, barW, bh, color, Stats.EDGE_LABELS[i], bins.get(i));
            }
            // x tick label (rotated)
            double lx = left + i * slot + slot / 2;
            double ly = top + plotH + 10;
            fmtln(g,
                    "<text x=\"%.2f\" y=\"%.2f\" class=\"tick\" text-anchor=\"end\" "
                            + "transform=\"rotate(-45 %.2f %.2f)\">%s</text>",
                    lx, ly, lx, ly, Stats.EDGE_LABELS[i]);
        }
        fmtln(g,
                "<text x=\"%.2f\" y=\"%.2f\" class=\"axis\">count (log scale) — peak %,d</text>",
                left, top - 3, max);
        g.append("</svg>");
        return g.toString();
    }

    // ---- SVG: latency-by-percentile "nines" plot ----
    private static String svgPercentiles(Stats s, String color) {
        final double left = 56;
        final double right = 16;
        final double top = 16;
        final double bottom = 44;
        double plotW = W - left - right;
        double plotH = HT - top - bottom;

        String[] labels = {"50%", "90%", "99%", "99.9%", "99.99%", "max"};
        double[] vals = {s.p50(), s.p90(), s.p99(), s.p999(), s.p9999(), s.maxMs()};
        double yMax = 0;
        for (double v : vals) {
            yMax = Math.max(yMax, v);
        }
        if (yMax <= 0) {
            yMax = 1;
        }
        yMax *= 1.1;

        StringBuilder g = new StringBuilder();
        g.append(SVG_OPEN);
        // y gridlines + labels (0, 25, 50, 75, 100 %)
        for (int t = 0; t <= 4; t++) {
            double v = yMax * t / 4.0;
            double y = top + plotH - plotH * (t / 4.0);
            g.append(line(left, y, left + plotW, y, "#eef1f5"));
            fmtln(g,
                    "<text x=\"%.2f\" y=\"%.2f\" class=\"tick\" text-anchor=\"end\">%s</text>",
                    left - 6, y + 3, num(v, v < 1 ? 3 : 2));
        }
        // polyline + points
        StringBuilder pts = new StringBuilder();
        double step = plotW / (labels.length - 1);
        for (int i = 0; i < vals.length; i++) {
            double x = left + i * step;
            double y = top + plotH - plotH * (vals[i] / yMax);
            pts.append(String.format(Locale.US, "%.2f,%.2f ", x, y));
        }
        fmtln(g,
                "<polyline points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"2\"/>",
                pts.toString().trim(), color);
        for (int i = 0; i < vals.length; i++) {
            double x = left + i * step;
            double y = top + plotH - plotH * (vals[i] / yMax);
            fmtln(g, "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"3.5\" fill=\"%s\"/>",
                    x, y, color);
            fmtln(g,
                    "<text x=\"%.2f\" y=\"%.2f\" class=\"pt\" text-anchor=\"middle\">%s</text>",
                    x, y - 8, num(vals[i], vals[i] < 1 ? 3 : 2));
            fmtln(g,
                    "<text x=\"%.2f\" y=\"%.2f\" class=\"tick\" text-anchor=\"middle\">%s</text>",
                    x, top + plotH + 16, labels[i]);
        }
        fmtln(g, "<text x=\"%.2f\" y=\"%.2f\" class=\"axis\">latency ms</text>",
                left - 48, top + plotH / 2);
        g.append("</svg>");
        return g.toString();
    }

    private static String line(double x1, double y1, double x2, double y2, String stroke) {
        return String.format(Locale.US,
                "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"1.0\"/>",
                x1, y1, x2, y2, stroke) + "\n";
    }

    private static void chip(StringBuilder h, String k, String v) {
        h.append("<span class=\"chip\"><b>").append(escape(k)).append("</b> ")
                .append(escape(v)).append("</span>\n");
    }

    private static void td(StringBuilder h, double ms) {
        h.append("<td>").append(num(ms, ms < 1 ? 3 : 2)).append("</td>");
    }

    private static String num(double v, int decimals) {
        return switch (decimals) {
            case 0 -> String.format(Locale.US, "%,.0f", v);
            case 3 -> String.format(Locale.US, "%,.3f", v);
            default -> String.format(Locale.US, "%,.2f", v);
        };
    }

    private static String num(long v) {
        return String.format(Locale.US, "%,d", v);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String CSS = """
                :root { color-scheme: light; }
                * { box-sizing: border-box; }
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial,
                       sans-serif; margin: 0; padding: 24px; background: #f6f7f9; color: #1a1a2e; }
                h1 { font-size: 22px; margin: 0 0 2px; }
                h2 { font-size: 17px; margin: 0 0 8px; display: flex; align-items: center; }
                h3 { font-size: 13px; font-weight: 600; color: #4b5563; margin: 0 0 4px; }
                .sub { color: #6b7280; margin: 0 0 18px; max-width: 90ch; }
                .category { font-size: 12px; text-transform: uppercase; letter-spacing: .07em;
                        color: #6b7280; margin: 24px 0 10px; border-bottom: 1px solid #e5e7eb;
                        padding-bottom: 5px; }
                .card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px 18px;
                        margin: 0 0 18px; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
                .desc { color: #6b7280; font-size: 13px; margin: 0 0 12px; max-width: 70ch; }
                .dot { width: 11px; height: 11px; border-radius: 50%; display: inline-block; margin-right: 8px; }
                table.env { border-collapse: collapse; font-size: 13px; }
                table.env th { text-align: left; color: #6b7280; font-weight: 500; padding: 2px 18px 2px 0;
                        white-space: nowrap; vertical-align: top; }
                table.env td { padding: 2px 0; font-variant-numeric: tabular-nums; }
                .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 14px; }
                .chip { background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 999px; padding: 3px 11px;
                        font-size: 12px; color: #334155; }
                .chip b { color: #64748b; font-weight: 500; }
                table.stats { border-collapse: collapse; width: 100%; font-size: 13px;
                        font-variant-numeric: tabular-nums; }
                table.stats th { color: #6b7280; font-weight: 500; text-align: right; padding: 4px 10px;
                        border-bottom: 1px solid #e5e7eb; }
                table.stats td { text-align: right; padding: 5px 10px; }
                .unit { color: #9ca3af; font-size: 11px; margin: 3px 0 0; text-align: right; }
                .charts { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 14px; }
                .chart { flex: 1 1 340px; min-width: 320px; }
                .svg { width: 100%; height: auto; background: #fff; border: 1px solid #f0f0f2;
                       border-radius: 6px; }
                .tick { font-size: 9px; fill: #6b7280; }
                .pt { font-size: 9px; fill: #374151; font-variant-numeric: tabular-nums; }
                .axis { font-size: 10px; fill: #9ca3af; }
                .foot { color: #9ca3af; font-size: 12px; margin-top: 8px; }
                code { background: #eef1f5; padding: 1px 5px; border-radius: 4px; font-size: 12px; }
                """;
}

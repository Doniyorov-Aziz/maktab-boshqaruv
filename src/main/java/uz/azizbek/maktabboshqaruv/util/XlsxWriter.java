package uz.azizbek.maktabboshqaruv.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A minimal real .xlsx (Office Open XML) with one sheet: a bold header row and text/number
 * cells. Enough for exports that open in Excel, LibreOffice and Google Sheets, without
 * pulling a spreadsheet library into the application.
 */
public final class XlsxWriter {

    private XlsxWriter() {
    }

    /** @param rows cell values: Number → numeric cell, anything else → text (null = empty) */
    public static byte[] write(String sheetName, List<String> header, List<Integer> widths, List<List<Object>> rows) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                    </Types>""");
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>""".formatted(escape(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName)));
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>""");
            put(zip, "xl/styles.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                    <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                    <borders count="1"><border/></borders>
                    <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                    <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
                    </styleSheet>""");
            put(zip, "xl/worksheets/sheet1.xml", sheet(header, widths, rows));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static String sheet(List<String> header, List<Integer> widths, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder(256 + rows.size() * 200);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        if (widths != null && !widths.isEmpty()) {
            sb.append("<cols>");
            for (int i = 0; i < widths.size(); i++) {
                sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                        .append("\" width=\"").append(widths.get(i)).append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols>");
        }
        sb.append("<sheetData>");
        row(sb, 1, List.copyOf(header), 1);
        int r = 2;
        for (List<Object> values : rows) row(sb, r++, values, 0);
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static void row(StringBuilder sb, int r, List<?> values, int style) {
        sb.append("<row r=\"").append(r).append("\">");
        for (int c = 0; c < values.size(); c++) {
            Object v = values.get(c);
            if (v == null) continue;
            String ref = column(c) + r;
            if (v instanceof Number n) {
                sb.append("<c r=\"").append(ref).append("\"").append(style > 0 ? " s=\"" + style + "\"" : "")
                        .append("><v>").append(n).append("</v></c>");
            } else {
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(style > 0 ? " s=\"" + style + "\"" : "")
                        .append("><is><t xml:space=\"preserve\">").append(escape(String.valueOf(v))).append("</t></is></c>");
            }
        }
        sb.append("</row>");
    }

    static String column(int index) {
        StringBuilder s = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            s.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return s.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    // XML 1.0 forbids most control characters
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

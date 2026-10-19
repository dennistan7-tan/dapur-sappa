package com.dapursappa.app.data

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Sel dengan gaya: 0 biasa, 1 judul kolom, 2 angka ribuan, 3 persen, 4 total tebal (angka), 5 total tebal (teks). */
class XC(val v: Any?, val style: Int = -1)

class XSheet(val name: String, val widths: List<Int>, val rows: List<List<Any?>>)

/** Penulis .xlsx sederhana (tanpa library): beberapa sheet, judul tebal, angka berformat. */
object Xlsx {
    private fun esc(s: String): String {
        val b = StringBuilder()
        for (ch in s) when (ch) {
            '&' -> b.append("&amp;"); '<' -> b.append("&lt;"); '>' -> b.append("&gt;"); '"' -> b.append("&quot;")
            else -> if (ch.code >= 32 || ch == '\n') b.append(ch)
        }
        return b.toString()
    }

    private fun col(i: Int): String {
        var n = i; val sb = StringBuilder()
        while (true) { sb.insert(0, ('A' + n % 26)); n = n / 26 - 1; if (n < 0) break }
        return sb.toString()
    }

    private fun sheetXml(sh: XSheet): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        sb.append("<cols>")
        sh.widths.forEachIndexed { i, w -> sb.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>") }
        sb.append("</cols><sheetData>")
        sh.rows.forEachIndexed { r, row ->
            sb.append("<row r=\"${r + 1}\">")
            row.forEachIndexed { c, raw ->
                val x = if (raw is XC) raw else XC(raw)
                val ref = col(c) + (r + 1)
                val v = x.v
                val header = r == 0
                when (v) {
                    null -> {}
                    is Number -> {
                        val st = if (x.style >= 0) x.style else 2
                        sb.append("<c r=\"$ref\" s=\"$st\"><v>${v.toDouble().let { d -> if (d == Math.floor(d) && Math.abs(d) < 1e15) d.toLong().toString() else d.toString() }}</v></c>")
                    }
                    else -> {
                        val st = if (x.style >= 0) x.style else if (header) 1 else 0
                        sb.append("<c r=\"$ref\" s=\"$st\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v.toString())}</t></is></c>")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private const val STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"0.0%\"/></numFmts>" +
        "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"4\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>" +
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFD6DE\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFF1D6\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
        "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>" +
        "<border><left style=\"thin\"><color rgb=\"FFCCCCCC\"/></left><right style=\"thin\"><color rgb=\"FFCCCCCC\"/></right><top style=\"thin\"><color rgb=\"FFCCCCCC\"/></top><bottom style=\"thin\"><color rgb=\"FFCCCCCC\"/></bottom><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"6\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>" +
        "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>" +
        "<xf numFmtId=\"3\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" +
        "</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>"

    fun write(out: OutputStream, sheets: List<XSheet>) {
        val z = ZipOutputStream(out)
        fun put(name: String, text: String) {
            z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry()
        }
        val ct = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        sheets.forEachIndexed { i, _ -> ct.append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>") }
        ct.append("</Types>")
        put("[Content_Types].xml", ct.toString())
        put("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
        val wb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>")
        val rel = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        sheets.forEachIndexed { i, s ->
            val nm = esc(s.name.replace(Regex("[\\\\/?*\\[\\]:]"), " ").take(31))
            wb.append("<sheet name=\"$nm\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
            rel.append("<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
        }
        wb.append("</sheets></workbook>")
        rel.append("<Relationship Id=\"rId${sheets.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>")
        put("xl/workbook.xml", wb.toString())
        put("xl/_rels/workbook.xml.rels", rel.toString())
        put("xl/styles.xml", STYLES)
        sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        z.finish()
        z.flush()
    }
}

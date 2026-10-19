/* Export laporan (.xlsx) dan brosur menu (PNG). Tanpa library. */
(function (root) {
  'use strict';
  const C = root.Core;
  const X = {};

  // ---------- zip (tanpa kompresi) ----------
  const CRC = (() => { const t = []; for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
  const crc32 = (b) => { let c = 0xFFFFFFFF; for (let i = 0; i < b.length; i++) c = CRC[(c ^ b[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; };
  function zip(files) { // files: [{name, text}]
    const enc = new TextEncoder(); const parts = []; const cen = []; let off = 0;
    const u16 = (v) => [v & 255, (v >>> 8) & 255]; const u32 = (v) => [v & 255, (v >>> 8) & 255, (v >>> 16) & 255, (v >>> 24) & 255];
    files.forEach((f) => {
      const nm = enc.encode(f.name), data = enc.encode(f.text), crc = crc32(data);
      const lh = new Uint8Array([0x50, 0x4b, 3, 4, 20, 0, 0, 8, 0, 0, 0, 0, 0x21, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(nm.length), 0, 0]);
      parts.push(lh, nm, data);
      cen.push(new Uint8Array([0x50, 0x4b, 1, 2, 20, 0, 20, 0, 0, 8, 0, 0, 0, 0, 0x21, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(nm.length), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ...u32(off)]), nm);
      off += lh.length + nm.length + data.length;
    });
    let cs = 0; cen.forEach((c) => { cs += c.length; });
    const end = new Uint8Array([0x50, 0x4b, 5, 6, 0, 0, 0, 0, ...u16(files.length), ...u16(files.length), ...u32(cs), ...u32(off), 0, 0]);
    return new Blob([...parts, ...cen, end], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  }

  // ---------- xlsx ----------
  // Sel: nilai biasa, atau {v, s} dengan s: 1 judul, 2 angka, 3 persen, 4 total angka, 5 total teks
  const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const colName = (i) => { let n = i, s = ''; for (;;) { s = String.fromCharCode(65 + (n % 26)) + s; n = Math.floor(n / 26) - 1; if (n < 0) break; } return s; };
  function sheetXml(sh) {
    let x = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>';
    sh.widths.forEach((w, i) => { x += `<col min="${i + 1}" max="${i + 1}" width="${w}" customWidth="1"/>`; });
    x += '</cols><sheetData>';
    sh.rows.forEach((row, r) => {
      x += `<row r="${r + 1}">`;
      row.forEach((raw, c) => {
        const cell = raw && typeof raw === 'object' ? raw : { v: raw, s: -1 };
        const v = cell.v; if (v === null || v === undefined) return;
        const ref = colName(c) + (r + 1);
        if (typeof v === 'number') x += `<c r="${ref}" s="${cell.s >= 0 ? cell.s : 2}"><v>${v}</v></c>`;
        else x += `<c r="${ref}" s="${cell.s >= 0 ? cell.s : (r === 0 ? 1 : 0)}" t="inlineStr"><is><t xml:space="preserve">${esc(v)}</t></is></c>`;
      });
      x += '</row>';
    });
    return x + '</sheetData></worksheet>';
  }
  const STYLES = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="0.0%"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="4"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFFFD6DE"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="FFFFF1D6"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFCCCCCC"/></left><right style="thin"><color rgb="FFCCCCCC"/></right><top style="thin"><color rgb="FFCCCCCC"/></top><bottom style="thin"><color rgb="FFCCCCCC"/></bottom><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="6"><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf><xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/><xf numFmtId="3" fontId="1" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/><xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>';
  X.xlsx = function (sheets) {
    const H = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
    let ct = H + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>';
    sheets.forEach((s, i) => { ct += `<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`; });
    ct += '</Types>';
    let wb = H + '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>';
    let rel = H + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">';
    sheets.forEach((s, i) => {
      wb += `<sheet name="${esc(s.name.replace(/[\\/?*\[\]:]/g, ' ').substring(0, 31))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>`;
      rel += `<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>`;
    });
    wb += '</sheets></workbook>';
    rel += `<Relationship Id="rId${sheets.length + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>`;
    const files = [
      { name: '[Content_Types].xml', text: ct },
      { name: '_rels/.rels', text: H + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>' },
      { name: 'xl/workbook.xml', text: wb }, { name: 'xl/_rels/workbook.xml.rels', text: rel }, { name: 'xl/styles.xml', text: STYLES },
    ];
    sheets.forEach((s, i) => files.push({ name: `xl/worksheets/sheet${i + 1}.xml`, text: sheetXml(s) }));
    return zip(files);
  };

  // ---------- isi laporan ----------
  const pctC = (p, r) => (r > 0 ? { v: p / r, s: 3 } : '');
  const per = (a, n) => (n > 0 ? Math.round(a / n) : '');
  const T = (v) => ({ v, s: 4 }), TT = (v) => ({ v, s: 5 });
  X.sheets = function (d) {
    const ds = [];
    d.orders.forEach((o) => ds.push(o.date));
    d.purchases.forEach((p) => { ds.push(p.date); if (p.kind === 'BAHAN' && p.useDate) ds.push(p.useDate); });
    d.expenses.forEach((e) => ds.push(e.date));
    const ok = ds.filter((s) => /^\d{4}-\d{2}-\d{2}$/.test(s)).sort();
    const today = C.today();
    const from = ok.length ? ok[0] : today; const to = ok.length && ok[ok.length - 1] > today ? ok[ok.length - 1] : today;
    const out = [];

    // bulan
    let rows = [['Bulan', 'Total pax', 'Omzet', 'Belanja bahan', 'Kemasan', 'Modal (bahan+kemasan)', 'Pengeluaran harian', 'Gross profit', 'Margin gross', 'Biaya bulanan', 'Profit bersih', 'Margin bersih', 'Modal per porsi', 'Harga jual rata-rata per porsi']];
    let m = C.firstOfMonth(from); let tp = 0, tr = 0, tb = 0, tk = 0, te = 0, tm = 0;
    while (m <= to) {
      const mc = C.month(d, m); const gross = mc.revenue - mc.modal - mc.expHarian;
      if (mc.revenue > 0 || mc.modal > 0 || mc.expHarian > 0 || mc.monthlyCost > 0) {
        rows.push([C.monthName(m), mc.pax, mc.revenue, mc.bahanCost, mc.kemasanCost, mc.modal, mc.expHarian, gross, pctC(gross, mc.revenue), mc.monthlyCost, mc.profit, pctC(mc.profit, mc.revenue), per(mc.modal, mc.pax), per(mc.revenue, mc.pax)]);
        tp += mc.pax; tr += mc.revenue; tb += mc.bahanCost; tk += mc.kemasanCost; te += mc.expHarian; tm += mc.monthlyCost;
      }
      m = C.addMonths(m, 1);
    }
    const tg = tr - tb - tk - te;
    rows.push([TT('TOTAL'), T(tp), T(tr), T(tb), T(tk), T(tb + tk), T(te), T(tg), tr > 0 ? { v: tg / tr, s: 3 } : '', T(tm), T(tg - tm), tr > 0 ? { v: (tg - tm) / tr, s: 3 } : '', tp > 0 ? T(Math.round((tb + tk) / tp)) : '', tp > 0 ? T(Math.round(tr / tp)) : '']);
    out.push({ name: 'Per Bulan', widths: [18, 10, 14, 14, 12, 16, 16, 14, 10, 14, 14, 10, 14, 18], rows });

    // minggu
    rows = [['Minggu ke', 'Periode', 'Total pax', 'Omzet', 'Belanja bahan', 'Kemasan', 'Modal (bahan+kemasan)', 'Pengeluaran harian', 'Gross profit', 'Margin gross', 'Modal per porsi', 'Harga jual rata-rata per porsi']];
    let wk = C.mondayOf(from);
    while (wk <= to) {
      const days = C.week(d, wk); const s = (f) => days.reduce((a, x) => a + f(x), 0);
      const rev = s((x) => x.revenue), pax = s((x) => x.pax), bh = s((x) => x.bahanCost), km = s((x) => x.kemasanCost), ex = s((x) => x.expCost);
      if (rev > 0 || bh > 0 || km > 0 || ex > 0) { const g = rev - bh - km - ex; rows.push(['W' + C.weekNo(wk) + ' ' + wk.substring(0, 4), C.range(wk, C.addDays(wk, 6)), pax, rev, bh, km, bh + km, ex, g, pctC(g, rev), per(bh + km, pax), per(rev, pax)]); }
      wk = C.addDays(wk, 7);
    }
    out.push({ name: 'Per Minggu', widths: [12, 28, 10, 14, 14, 12, 16, 16, 14, 10, 14, 18], rows });

    // hari
    rows = [['Tanggal', 'Hari', 'Menu', 'Total pax', 'Omzet', 'Belanja bahan (harga bahan hari itu)', 'Kemasan', 'Modal', 'Pengeluaran harian', 'Gross profit', 'Margin gross', 'Modal per porsi', 'Harga jual per porsi']];
    let day = from;
    while (day <= to) {
      const dc = C.day(d, day);
      if (dc.pax > 0 || dc.revenue > 0 || dc.bahanCost > 0 || dc.expCost > 0) rows.push([day, C.dayName(day), C.menuLines(dc.menuName).join(' · '), dc.pax, dc.revenue, dc.bahanCost, dc.kemasanCost, dc.cost, dc.expCost, dc.profit, pctC(dc.profit, dc.revenue), per(dc.cost, dc.pax), per(dc.revenue, dc.pax)]);
      day = C.addDays(day, 1);
    }
    out.push({ name: 'Per Hari', widths: [12, 10, 36, 10, 14, 18, 12, 14, 16, 14, 10, 14, 16], rows });

    // pengeluaran (semua record + kategori)
    const recs = [];
    const nm = (by) => (by < 0 ? 'Developer' : (d.users[by] ? d.users[by].name : 'User ' + (by + 1)));
    d.purchases.forEach((p) => recs.push({ date: p.date, cat: C.KINDS[p.kind] || p.kind, name: p.name, qty: (C.num(p.qty) + ' ' + (p.unit || '')).trim(), amount: p.price, use: p.kind === 'BAHAN' ? (p.useDate || '') : '', charge: p.kind === 'BAHAN' ? 'Modal harian (bahan)' : p.kind === 'KEMASAN' ? 'Modal harian (kemasan)' : p.kind === 'BULANAN' ? 'Biaya bulanan' : 'Profit harian', by: nm(p.by) }));
    d.expenses.forEach((e) => recs.push({ date: e.date, cat: e.monthly ? 'Biaya bulanan' : 'Pengeluaran lain', name: e.name, qty: '', amount: e.amount, use: '', charge: e.monthly ? 'Biaya bulanan' : 'Profit harian', by: nm(e.by) }));
    recs.sort((a, b) => (a.date + a.cat < b.date + b.cat ? -1 : 1));
    rows = [['Tanggal', 'Kategori', 'Nama / item', 'Jumlah', 'Total (Rp)', 'Dipakai untuk tanggal', 'Dibebankan ke', 'Dicatat oleh']];
    recs.forEach((r) => rows.push([r.date, r.cat, r.name, r.qty, r.amount, r.use, r.charge, r.by]));
    rows.push([TT('TOTAL'), TT(''), TT(''), TT(''), T(recs.reduce((a, r) => a + r.amount, 0))]);
    out.push({ name: 'Pengeluaran', widths: [12, 20, 30, 12, 14, 18, 24, 14], rows });

    // order
    const key = (o) => o.time || C.slotTime(o.slot) || '99:99';
    rows = [['Tanggal', 'Jam antar', 'Waktu', 'Customer', 'Lokasi', 'Pax', 'Harga/porsi', 'Tambahan', 'Total', 'Status bayar', 'Remarks']];
    d.orders.slice().sort((a, b) => (a.date + key(a) + a.customer.toLowerCase() < b.date + key(b) + b.customer.toLowerCase() ? -1 : 1)).forEach((o) => {
      rows.push([o.date, o.time || '', o.slot || '', o.customer, o.location || '', C.orderPortions(o), o.price || 0, C.extrasTotal(o), C.orderTotal(o), o.paid ? 'Lunas' : 'Belum bayar', o.remarks || '']);
    });
    out.push({ name: 'Order', widths: [12, 10, 10, 22, 18, 8, 12, 12, 14, 14, 24], rows });
    return out;
  };

  function download(blob, name) {
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = name;
    document.body.appendChild(a); a.click(); setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1500);
  }
  X.downloadReport = (d) => download(X.xlsx(X.sheets(d)), 'Laporan-DapurSappa-' + C.today() + '.xlsx');

  // ---------- brosur menu ----------
  const MON = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
  X.brosurDate = (mon) => {
    const fri = C.addDays(mon, 4); const a = mon.split('-'), b = fri.split('-');
    return a[1] === b[1] ? `${+a[2]} – ${+b[2]} ${MON[+b[1] - 1]} ${b[0]}` : `${+a[2]} ${MON[+a[1] - 1]} – ${+b[2]} ${MON[+b[1] - 1]} ${b[0]}`;
  };
  let tpl = null;
  X.brosurCanvas = function (d, mon, cb) {
    const draw = () => {
      const cv = document.createElement('canvas'); cv.width = 1080; cv.height = 1920;
      const g = cv.getContext('2d'); g.drawImage(tpl, 0, 0, 1080, 1920);
      g.fillStyle = '#5A3A22'; g.strokeStyle = '#5A3A22'; g.lineJoin = 'round'; g.textAlign = 'center'; g.textBaseline = 'middle';
      const font = (px) => `${px}px LazyDog, "Arial Black", "Segoe UI Black", sans-serif`;
      g.font = font(62); g.lineWidth = 3.2; g.strokeText(X.brosurDate(mon), 540, 480); g.fillText(X.brosurDate(mon), 540, 480);
      const xs = [195, 545, 903, 195, 545], ys = [792, 792, 792, 1130, 1130];
      const all = [];
      for (let i = 0; i < 5; i++) {
        const dc = C.day(d, C.addDays(mon, i));
        let lines = C.menuLines(dc.menuName).map((s) => s.toUpperCase()).slice(0, 3);
        if (!lines.length && dc.libur) lines = ['LIBUR'];
        all.push(lines);
      }
      let size = 40; g.font = font(size);
      while (size > 18 && all.some((ls) => ls.some((l) => g.measureText(l).width > 340))) { size -= 1; g.font = font(size); }
      g.lineWidth = size * 0.07;
      all.forEach((lines, i) => {
        lines.forEach((l, k) => { const y = ys[i] + (k - (lines.length - 1) / 2) * 50; g.strokeText(l, xs[i], y); g.fillText(l, xs[i], y); });
      });
      cb(cv);
    };
    const go = () => { const p = (document.fonts && document.fonts.load) ? document.fonts.load('36px LazyDog').catch(() => {}) : Promise.resolve(); p.then(draw); };
    if (tpl) go(); else { tpl = new Image(); tpl.onload = go; tpl.src = 'brosur.jpg'; }
  };
  X.fontOk = () => !!(document.fonts && document.fonts.check && document.fonts.check('36px LazyDog'));
  X.downloadBrosur = (cv, mon) => cv.toBlob((b) => download(b, 'Menu-DapurSappa-' + mon + '.png'), 'image/png');

  root.Export = X;
})(window);

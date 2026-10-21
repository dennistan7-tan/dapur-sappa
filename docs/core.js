/* Dapur Sappa web · logika inti (sama dengan aplikasi Android). */
(function (root) {
  'use strict';
  const C = {};

  // ---------- format & tanggal ----------
  const DAYS = ['Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu', 'Minggu'];
  const MON = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];
  const MONFULL = ['Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'];
  const MON_EN = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  C.DAYS = DAYS;

  const pad = (n) => (n < 10 ? '0' : '') + n;
  const toD = (s) => { const p = s.split('-'); return new Date(Date.UTC(+p[0], +p[1] - 1, +p[2])); };
  const fromD = (d) => d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate());
  C.today = () => { const n = new Date(); return n.getFullYear() + '-' + pad(n.getMonth() + 1) + '-' + pad(n.getDate()); };
  C.addDays = (s, n) => { const d = toD(s); d.setUTCDate(d.getUTCDate() + n); return fromD(d); };
  C.dow = (s) => { const g = toD(s).getUTCDay(); return g === 0 ? 7 : g; }; // 1=Senin..7=Minggu
  C.mondayOf = (s) => C.addDays(s, -(C.dow(s) - 1));
  C.dayName = (s) => DAYS[C.dow(s) - 1];
  C.dayNum = (s) => +s.split('-')[2];
  C.dm = (s) => { const p = s.split('-'); return (+p[2]) + ' ' + MON[+p[1] - 1]; };
  C.dayLong = (s) => C.dayName(s) + ', ' + C.dm(s) + ' ' + s.split('-')[0];
  C.dayShort = (s) => C.dayName(s).substring(0, 3) + ', ' + C.dm(s);
  C.monthName = (s) => { const p = s.split('-'); return MONFULL[+p[1] - 1] + ' ' + p[0]; };
  C.monthAbbr = (s) => MON[+s.split('-')[1] - 1];
  C.firstOfMonth = (s) => s.substring(0, 8) + '01';
  C.addMonths = (s, n) => { const d = toD(C.firstOfMonth(s)); d.setUTCMonth(d.getUTCMonth() + n); return fromD(d); };
  C.monthLen = (s) => { const d = toD(C.firstOfMonth(s)); return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate(); };
  C.weekNo = (s) => {
    const d = toD(s); const day = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - day);
    const y0 = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil(((d - y0) / 86400000 + 1) / 7);
  };
  C.range = (a, b) => {
    const f = (s) => { const p = s.split('-'); return p[2] + ' ' + MON_EN[+p[1] - 1] + ' ' + p[0]; };
    return f(a) + ' – ' + f(b);
  };
  C.rp = (v) => (v < 0 ? '-Rp' : 'Rp') + Math.abs(Math.round(v)).toLocaleString('id-ID');
  C.th = (v) => Math.round(v).toLocaleString('id-ID');
  C.num = (d) => (d === Math.floor(d) ? String(d) : String(Math.round(d * 100) / 100).replace('.', ','));
  C.timeOf = (ts) => { const d = new Date(ts); return pad(d.getHours()) + '.' + pad(d.getMinutes()); };
  C.dateOfTs = (ts) => { const d = new Date(ts); return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); };
  C.uuid = () => (root.crypto && root.crypto.randomUUID) ? root.crypto.randomUUID()
    : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => { const r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16); });

  // ---------- SHA-256 (sinkron, tanpa crypto.subtle) ----------
  C.sha256 = function (str) {
    const bytes = unescape(encodeURIComponent(str)).split('').map((c) => c.charCodeAt(0));
    const K = [0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2];
    const H = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];
    const l = bytes.length * 8;
    bytes.push(0x80);
    while (bytes.length % 64 !== 56) bytes.push(0);
    for (let i = 7; i >= 0; i--) bytes.push(i >= 4 ? 0 : (l >>> (i * 8)) & 0xff);
    const rotr = (x, n) => (x >>> n) | (x << (32 - n));
    for (let o = 0; o < bytes.length; o += 64) {
      const w = new Array(64);
      for (let i = 0; i < 16; i++) w[i] = (bytes[o + i * 4] << 24) | (bytes[o + i * 4 + 1] << 16) | (bytes[o + i * 4 + 2] << 8) | bytes[o + i * 4 + 3];
      for (let i = 16; i < 64; i++) {
        const s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >>> 3);
        const s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >>> 10);
        w[i] = (w[i - 16] + s0 + w[i - 7] + s1) | 0;
      }
      let [a, b, c, d, e, f, g, h] = H;
      for (let i = 0; i < 64; i++) {
        const S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
        const ch = (e & f) ^ (~e & g);
        const t1 = (h + S1 + ch + K[i] + w[i]) | 0;
        const S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
        const maj = (a & b) ^ (a & c) ^ (b & c);
        const t2 = (S0 + maj) | 0;
        h = g; g = f; f = e; e = (d + t1) | 0; d = c; c = b; b = a; a = (t1 + t2) | 0;
      }
      H[0] = (H[0] + a) | 0; H[1] = (H[1] + b) | 0; H[2] = (H[2] + c) | 0; H[3] = (H[3] + d) | 0;
      H[4] = (H[4] + e) | 0; H[5] = (H[5] + f) | 0; H[6] = (H[6] + g) | 0; H[7] = (H[7] + h) | 0;
    }
    return H.map((x) => ('00000000' + (x >>> 0).toString(16)).slice(-8)).join('');
  };

  // ---------- parser belanja ----------
  const PRICE_SUFFIX = { rb: 1000, ribu: 1000, k: 1000, jt: 1e6, juta: 1e6 };
  const UNITS = {
    kg: 'kg', kilo: 'kg', kgs: 'kg', g: 'g', gr: 'g', gram: 'g', ons: 'ons', ml: 'ml', l: 'l', ltr: 'l', liter: 'l',
    pcs: 'pcs', pc: 'pcs', biji: 'pcs', buah: 'pcs', bh: 'pcs', butir: 'butir', ikat: 'ikat', bungkus: 'bungkus', bks: 'bungkus',
    pack: 'pack', pak: 'pack', papan: 'papan', sisir: 'sisir', lembar: 'lembar', botol: 'botol', kotak: 'kotak', dus: 'dus',
    karung: 'karung', siung: 'siung', porsi: 'porsi', ekor: 'ekor', potong: 'potong', sachet: 'sachet', renteng: 'renteng',
    batang: 'batang', tabung: 'tabung', roll: 'roll', gulung: 'gulung', kaleng: 'kaleng', ruas: 'ruas', tangkai: 'tangkai',
    genggam: 'genggam', bonggol: 'bonggol', slop: 'slop', bal: 'bal', box: 'box',
  };
  const DROP = ['rp', 'rp.', 'seharga', 'harga', 'beli', 'isi', 'x', '@', 'total', 'sebanyak'];
  const KEMASAN_WORDS = ['sterofoam', 'styrofoam', 'steroform', 'stereofoam', 'styrofom', 'sterofom', 'mika', 'cup', 'kertas nasi', 'sendok', 'garpu', 'sedotan', 'wadah', 'paper bowl', 'box', 'kotak nasi', 'tempat makan', 'alumunium foil', 'aluminium foil', 'foil'];
  const BULANAN_WORDS = ['kantong', 'kresek', 'elpiji', 'lpg', 'tisu', 'tissue', 'karet', 'tali', 'sabun', 'spons', 'lakban', 'isolasi', 'plastik', 'stiker', 'label', 'korek'];
  const LAIN_WORDS = ['bensin', 'bbm', 'pertalite', 'tambal', 'parkir', 'servis', 'service', 'ongkir', 'ojek', 'gojek', 'grab', 'tol ', 'oli'];
  C.KINDS = { BAHAN: 'Bahan harian', KEMASAN: 'Kemasan', BULANAN: 'Biaya bulanan', LAIN: 'Pengeluaran lain' };

  C.detectKind = function (name) {
    const n = name.toLowerCase();
    for (const w of LAIN_WORDS) if ((n + ' ').indexOf(w) >= 0) return 'LAIN';
    if (n.split(' ').indexOf('gas') >= 0) return 'BULANAN';
    for (const w of KEMASAN_WORDS) if (n.indexOf(w) >= 0) return 'KEMASAN';
    for (const w of BULANAN_WORDS) if (n.indexOf(w) >= 0) return 'BULANAN';
    return 'BAHAN';
  };
  function parseNum(s) {
    const t = s.replace(/^[.,]+|[.,]+$/g, '');
    if (!t) return null;
    if (/^\d{1,3}([.,]\d{3})+$/.test(t)) return parseFloat(t.replace(/[.,]/g, ''));
    const v = parseFloat(t.replace(',', '.'));
    return isNaN(v) ? null : v;
  }
  C.parse = function (text) {
    const clean = text.toLowerCase().replace(/\n/g, ' ').trim();
    if (!clean) return null;
    const tokens = clean.split(/\s+/).filter(Boolean);
    const nameParts = [];
    let explicitPrice = null, qty = null, unit = '';
    const bare = [];
    let i = 0;
    while (i < tokens.length) {
      const tk = tokens[i];
      const next = i + 1 < tokens.length ? tokens[i + 1] : null;
      if ((tk === 'rp' || tk === 'rp.') && next !== null && /^[\d.,]*\d$/.test(next)) {
        const v = parseNum(next);
        if (v !== null) {
          const after = i + 2 < tokens.length ? tokens[i + 2] : null;
          const mult = after !== null ? PRICE_SUFFIX[after] : undefined;
          if (mult) { explicitPrice = Math.round(v * mult); i += 3; } else { explicitPrice = Math.round(v); i += 2; }
          continue;
        }
      }
      const m = /^(rp\.?)?([\d.,]*\d)([a-z]*)$/.exec(tk);
      if (m) {
        const hasRp = !!m[1];
        const v = parseNum(m[2]);
        const letters = m[3];
        if (v !== null) {
          if (letters === '') {
            const mult = next !== null ? PRICE_SUFFIX[next] : undefined;
            const u = next !== null ? UNITS[next] : undefined;
            if (mult) { explicitPrice = Math.round(v * mult); i += 2; continue; }
            else if (u && !hasRp) { if (qty === null) { qty = v; unit = u; } else nameParts.push(tk); i += 2; continue; }
            else if (hasRp) { explicitPrice = Math.round(v); i++; continue; }
            else { bare.push(v); i++; continue; }
          } else {
            const mult = PRICE_SUFFIX[letters];
            const u = UNITS[letters];
            if (mult) { explicitPrice = Math.round(v * mult); i++; continue; }
            else if (u) { if (qty === null) { qty = v; unit = u; } else nameParts.push(tk); i++; continue; }
          }
        }
      }
      if (DROP.indexOf(tk) < 0) nameParts.push(tk);
      i++;
    }
    let price = explicitPrice === null ? 0 : explicitPrice;
    if (explicitPrice === null) {
      const bigs = bare.filter((x) => x >= 500);
      if (bigs.length) { const big = Math.max.apply(null, bigs); price = Math.round(big); bare.splice(bare.indexOf(big), 1); }
    }
    if (qty === null) {
      const small = bare.find((x) => x < 500 || explicitPrice !== null);
      if (small !== undefined) { qty = small; unit = 'pcs'; }
    }
    let name = nameParts.join(' ').trim();
    name = name.charAt(0).toUpperCase() + name.slice(1);
    return { name, qty: qty === null ? 1 : qty, unit, price, kind: C.detectKind(name), ok: name !== '' && price > 0 };
  };

  // ---------- model & kalkulasi ----------
  C.defaultUsers = () => [{ name: 'User', role: 'User' }];
  // waktu pengantaran
  C.SLOTS = [['PAGI', '07:00'], ['SIANG', '12:00'], ['SORE', '16:00'], ['MALAM', '19:00']];
  C.slotTime = (s) => { const x = C.SLOTS.find((q) => q[0] === s); return x ? x[1] : ''; };
  C.deliv = (o) => (o.slot ? o.slot + (o.time ? ' · ' + o.time : '') : (o.time || ''));
  C.pct = (profit, rev) => (rev > 0 ? (Math.round(profit / rev * 1000) / 10).toString().replace('.', ',') + '%' : '-');
  C.orderKey = (o) => o.time || C.slotTime(o.slot) || '99:99';
  C.orderPortions = (o) => (o.only ? 0 : (o.pax || 0));
  C.extrasTotal = (o) => (o.extras || []).reduce((s, e) => s + (e.q || 0) * (e.p || 0), 0);
  C.orderTotal = (o) => C.orderPortions(o) * (o.price || 0) + C.extrasTotal(o);

  C.buildData = function (recs) {
    const d = { purchases: [], orders: [], menus: {}, expenses: [], audits: [], users: C.defaultUsers() };
    Object.keys(recs).forEach((id) => {
      const o = recs[id];
      switch (o.t) {
        case 'p': if (!o.del) d.purchases.push(o); break;
        case 'o': if (!o.del) d.orders.push(o); break;
        case 'm': {
          const days = [], libur = [];
          for (let i = 0; i < 7; i++) {
            days.push((o.days && o.days[i]) || '');
            libur.push(o.libur && o.libur[i] !== undefined ? !!o.libur[i] : i === 6);
          }
          d.menus[o.week] = { week: o.week, days, libur, price: o.price || 20000, id: o.id };
          break;
        }
        case 'e': if (!o.del) d.expenses.push(o); break;
        case 'a': d.audits.push(o); break;
        case 'c': if (o.users && o.users.length) d.users = o.users.slice(0, 1).map((u) => ({ name: !(u.name || '').trim() || /^User \d+$/.test(u.name.trim()) ? 'User' : u.name, role: 'User' })); break;
      }
    });
    d.audits.sort((a, b) => b.ts - a.ts);
    return d;
  };

  C.menuOf = (d, date) => d.menus[C.mondayOf(date)] || null;
  C.menuRaw = (d, date) => { const m = C.menuOf(d, date); return m ? (m.days[C.dow(date) - 1] || '') : ''; };
  C.menuLines = (s) => String(s || '').split('\n').map((x) => x.trim()).filter((x) => x);
  C.menuName = (d, date) => C.menuLines(C.menuRaw(d, date)).join(' · ');
  C.isLibur = (d, date) => { const m = C.menuOf(d, date); return m ? m.libur[C.dow(date) - 1] : C.dow(date) === 7; };
  C.defaultPrice = (d, date) => {
    const m = C.menuOf(d, date); if (m) return m.price;
    const ks = Object.keys(d.menus).sort(); return ks.length ? d.menus[ks[ks.length - 1]].price : 20000;
  };

  C.day = function (d, date) {
    const orders = d.orders.filter((o) => o.date === date).sort((a, b) => { const x = C.orderKey(a) + a.customer.toLowerCase(), y = C.orderKey(b) + b.customer.toLowerCase(); return x < y ? -1 : x > y ? 1 : 0; });
    const pax = orders.reduce((s, o) => s + C.orderPortions(o), 0);
    const revenue = orders.reduce((s, o) => s + C.orderTotal(o), 0);
    const bahan = d.purchases.filter((p) => p.kind === 'BAHAN' && (p.useDate || p.date) === date);
    const bahanCost = bahan.reduce((s, p) => s + p.price, 0);
    const kemasan = [];
    if (pax > 0) {
      const groups = {};
      d.purchases.filter((p) => p.kind === 'KEMASAN' && p.qty > 0 && p.date <= date).forEach((p) => {
        const k = p.name.toLowerCase().trim();
        if (!groups[k] || p.date > groups[k].date) groups[k] = p;
      });
      Object.keys(groups).forEach((k) => {
        const p = groups[k]; const unit = p.price / p.qty;
        kemasan.push({ name: p.name, unitPrice: unit, pcs: pax, cost: Math.round(unit * pax) });
      });
    }
    const kemasanCost = kemasan.reduce((s, k) => s + k.cost, 0);
    const expenses = d.expenses.filter((e) => e.date === date && !e.monthly);
    const expCost = expenses.reduce((s, e) => s + e.amount, 0);
    const cost = bahanCost + kemasanCost;
    return {
      date, menuName: C.menuRaw(d, date), libur: C.isLibur(d, date), orders, pax, revenue, bahan, bahanCost,
      kemasan, kemasanCost, expenses, expCost, cost, profit: revenue - cost - expCost, costPerPax: pax > 0 ? cost / pax : 0,
    };
  };
  // Laporan per customer untuk satu bulan: order, pax, omzet, profit (omzet - modal bahan/kemasan per porsi), status bayar per minggu.
  C.customers = function (d, any) {
    const prefix = C.firstOfMonth(any).substring(0, 7);
    const cache = {}; const dayOf = (dt) => cache[dt] || (cache[dt] = C.day(d, dt));
    const map = {};
    d.orders.filter((o) => o.date.indexOf(prefix) === 0).forEach((o) => {
      const key = o.customer.trim().toLowerCase();
      const c = map[key] || (map[key] = { key, name: o.customer.trim(), orders: [], pax: 0, revenue: 0, cost: 0, weeks: {} });
      const por = C.orderPortions(o); const rev = C.orderTotal(o);
      c.orders.push(o); c.pax += por; c.revenue += rev; c.cost += por * dayOf(o.date).costPerPax;
      const wk = C.mondayOf(o.date);
      const w = c.weeks[wk] || (c.weeks[wk] = { week: wk, orders: [], revenue: 0, pax: 0, unpaid: [], methods: [] });
      w.orders.push(o); w.revenue += rev; w.pax += por;
      if (rev > 0) { if (!o.paid) w.unpaid.push(o); else if (o.pm && w.methods.indexOf(o.pm) < 0) w.methods.push(o.pm); }
    });
    const list = Object.keys(map).map((k) => {
      const c = map[k];
      c.profit = Math.round(c.revenue - c.cost);
      c.weekList = Object.keys(c.weeks).sort().map((x) => c.weeks[x]);
      c.weekList.forEach((w) => { w.paid = w.unpaid.length === 0; w.unpaidSum = w.unpaid.reduce((s, o) => s + C.orderTotal(o), 0); });
      c.unpaidSum = c.weekList.reduce((s, w) => s + w.unpaidSum, 0);
      c.allPaid = c.unpaidSum === 0;
      return c;
    }).sort((a, b) => b.revenue - a.revenue || (a.name < b.name ? -1 : 1));
    const sum = (f) => list.reduce((s, c) => s + f(c), 0);
    return {
      list, customers: list.length, orders: sum((c) => c.orders.length), pax: sum((c) => c.pax), revenue: sum((c) => c.revenue),
      profit: sum((c) => c.profit), unpaid: sum((c) => c.unpaidSum), paid: sum((c) => c.revenue) - sum((c) => c.unpaidSum),
    };
  };
  C.week = (d, monday) => { const r = []; for (let i = 0; i < 7; i++) r.push(C.day(d, C.addDays(monday, i))); return r; };
  C.month = function (d, any) {
    const first = C.firstOfMonth(any); const len = C.monthLen(first);
    const days = []; for (let i = 0; i < len; i++) days.push(C.day(d, C.addDays(first, i)));
    const prefix = first.substring(0, 7);
    const lines = {};
    d.purchases.forEach((p) => { if (p.kind === 'BULANAN' && p.date.indexOf(prefix) === 0) { const k = p.name.trim(); lines[k] = (lines[k] || 0) + p.price; } });
    d.expenses.forEach((e) => { if (e.monthly && e.date.indexOf(prefix) === 0) { const k = e.name.trim(); lines[k] = (lines[k] || 0) + e.amount; } });
    const monthlyLines = Object.keys(lines).map((k) => [k, lines[k]]);
    const monthlyCost = monthlyLines.reduce((s, x) => s + x[1], 0);
    const sum = (f) => days.reduce((s, x) => s + f(x), 0);
    return {
      first, days, revenue: sum((x) => x.revenue), bahanCost: sum((x) => x.bahanCost), kemasanCost: sum((x) => x.kemasanCost),
      modal: sum((x) => x.cost), expHarian: sum((x) => x.expCost), monthlyLines, monthlyCost,
      profit: sum((x) => x.profit) - monthlyCost, pax: sum((x) => x.pax),
      unpaid: d.orders.filter((o) => o.date.indexOf(prefix) === 0 && !o.paid && C.orderTotal(o) > 0),
    };
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = C; else root.Core = C;
})(typeof window !== 'undefined' ? window : globalThis);

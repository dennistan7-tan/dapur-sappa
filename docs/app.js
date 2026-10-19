/* Dapur Sappa · versi web. Memakai data Firebase yang sama dengan aplikasi Android. */
(function () {
  'use strict';
  const C = window.Core, Store = window.Store, CFG = window.SAPPA_CONFIG, LS = Store.LS;
  const LOGO = window.LOGO || 'logo.png';
  const $ = (s) => document.querySelector(s);
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const D = () => Store.data;
  const digits = (s) => String(s || '').replace(/\D/g, '').substring(0, 12);
  const today = () => C.today();

  const st = {
    tab: null, belDate: null, orderDate: null, weekStart: null, repTab: 2, repMonth: null, repWeek: null,
    route: null, f: {}, m: null, modal: null, rf: 'Semua',
  };

  // ---------- user ----------
  function getMe() {
    if (LS.get('ds_dev', '') === '1') return { idx: -1, isDev: true, name: 'Developer', role: 'Developer', label: 'Developer', canAdmin: true };
    const i = +LS.get('ds_user', '-1');
    const u = D().users[i];
    if (!u) return null;
    return { idx: i, isDev: false, name: u.name, role: 'User', label: 'User', canAdmin: true };
  }
  const canEdit = () => true;
  const userName = (idx) => (idx < 0 ? 'Developer' : (D().users[idx] ? D().users[idx].name : 'User ' + (idx + 1)));
  const devHash = () => LS.get('ds_devhash', '') || CFG.DEV_DEFAULT_HASH;
  const isDark = () => {
    const t = document.documentElement.getAttribute('data-theme');
    if (t) return t === 'dark';
    return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  };

  // ---------- repo (semua perubahan tercatat di riwayat) ----------
  function log(me, cat, title, detail) {
    Store.put({ t: 'a', id: C.uuid(), ts: Date.now(), who: me.name, cat, title, detail });
  }
  const kindLabel = (k) => C.KINDS[k] || k;
  const purchaseLine = (p) => (C.num(p.qty) + ' ' + p.unit).trim() + ' · ' + C.rp(p.price) + ' · ' + kindLabel(p.kind);
  function savePurchase(p, old, me) {
    Store.put(p);
    if (!old) return log(me, 'Belanja', 'Tambah belanja: ' + p.name, purchaseLine(p));
    const ch = [];
    if (old.name !== p.name) ch.push('Nama ' + old.name + ' → ' + p.name);
    if (old.price !== p.price) ch.push('Harga ' + C.rp(old.price) + ' → ' + C.rp(p.price));
    if (old.qty !== p.qty || old.unit !== p.unit) ch.push('Jumlah ' + C.num(old.qty) + ' ' + old.unit + ' → ' + C.num(p.qty) + ' ' + p.unit);
    if (old.kind !== p.kind) ch.push('Jenis ' + kindLabel(old.kind) + ' → ' + kindLabel(p.kind));
    if (old.useDate !== p.useDate) ch.push('Untuk tanggal ' + old.useDate + ' → ' + p.useDate);
    if (ch.length) log(me, 'Belanja', 'Edit belanja: ' + p.name, ch.join('\n'));
  }
  function deletePurchase(p, me) {
    Store.put(Object.assign({}, p, { del: true }));
    log(me, 'Belanja', 'Hapus belanja: ' + p.name, purchaseLine(p) + ' · Dihapus');
  }
  const orderLine = (o) => (o.only ? 'Lauk saja' : o.pax + ' pax') + ' · ' + C.rp(C.orderTotal(o)) + ' · ' + (o.paid ? 'Lunas' : 'Belum bayar');
  function saveOrder(o, old, me) {
    Store.put(o);
    if (!old) return log(me, 'Order', 'Tambah order ' + o.customer, orderLine(o));
    const ch = [];
    if (old.customer !== o.customer) ch.push('Nama ' + old.customer + ' → ' + o.customer);
    if (old.location !== o.location) ch.push('Lokasi ' + old.location + ' → ' + o.location);
    if (old.pax !== o.pax) ch.push('Pax ' + old.pax + ' → ' + o.pax);
    if (old.price !== o.price) ch.push('Harga porsi ' + C.rp(old.price) + ' → ' + C.rp(o.price));
    if (C.extrasTotal(old) !== C.extrasTotal(o)) ch.push('Tambahan ' + C.rp(C.extrasTotal(old)) + ' → ' + C.rp(C.extrasTotal(o)));
    if (old.paid !== o.paid) ch.push(o.paid ? 'Belum bayar → Lunas' : 'Lunas → Belum bayar');
    if (old.only !== o.only) ch.push(o.only ? 'Jadi lauk saja' : 'Jadi porsi');
    if (old.date !== o.date) ch.push('Tanggal ' + old.date + ' → ' + o.date);
    if (old.remarks !== o.remarks) ch.push('Remarks diubah');
    if (C.deliv(old) !== C.deliv(o)) ch.push('Waktu antar ' + (C.deliv(old) || '-') + ' → ' + (C.deliv(o) || '-'));
    if (ch.length) log(me, 'Order', 'Edit order ' + o.customer, ch.join('\n'));
  }
  function saveOrders(list, me) {
    list.forEach((o) => Store.put(o));
    if (list.length === 1) return log(me, 'Order', 'Tambah order ' + list[0].customer, orderLine(list[0]) + (C.deliv(list[0]) ? ' · ' + C.deliv(list[0]) : ''));
    log(me, 'Order', 'Tambah order ' + list[0].customer + ' · ' + list.length + ' hari', list.map((o) => C.dayName(o.date) + ' ' + C.dm(o.date)).join(', ') + '\n' + orderLine(list[0]) + ' per hari' + (C.deliv(list[0]) ? ' · ' + C.deliv(list[0]) : ''));
  }
  function deleteOrder(o, me) {
    Store.put(Object.assign({}, o, { del: true }));
    log(me, 'Order', 'Hapus order ' + o.customer, orderLine(o) + ' · Dihapus');
  }
  function saveExpense(e, me) {
    Store.put(e);
    log(me, 'Pengeluaran', 'Tambah pengeluaran: ' + e.name, C.rp(e.amount) + ' · ' + (e.monthly ? 'Profit Bulanan' : 'Profit Harian'));
  }
  function deleteExpense(e, me) {
    Store.put(Object.assign({}, e, { del: true }));
    log(me, 'Pengeluaran', 'Hapus pengeluaran: ' + e.name, C.rp(e.amount) + ' · Dihapus');
  }
  function saveMenu(m, old, me) {
    Store.put({ t: 'm', id: 'menu-' + m.week, week: m.week, days: m.days, libur: m.libur, price: m.price });
    const base = old || { days: ['', '', '', '', '', '', ''], libur: [false, false, false, false, false, false, true], price: 20000 };
    const ch = [];
    for (let i = 0; i < 7; i++) {
      const dn = C.DAYS[i];
      if (base.days[i] !== m.days[i]) ch.push(dn + ': ' + (base.days[i].replace(/\n/g, ' / ') || 'Belum diisi') + ' → ' + (m.days[i].replace(/\n/g, ' / ') || 'Belum diisi'));
      if (base.libur[i] !== m.libur[i]) ch.push(dn + ': ' + (m.libur[i] ? 'Libur' : 'Buka'));
    }
    if (base.price !== m.price) ch.push('Harga porsi ' + C.rp(base.price) + ' → ' + C.rp(m.price));
    if (ch.length) log(me, 'Menu', 'Ubah menu Week ' + C.weekNo(m.week), ch.join('\n'));
  }
  function saveUsers(users, me) {
    Store.put({ t: 'c', id: 'cfg', users });
    log(me, 'User', 'Ubah nama user', users.map((u) => u.name).join(', '));
  }

  // ---------- komponen html ----------
  const tile = (l, v, hi, t) => `<div class="tile${hi ? ' hi' : ''}${t ? ' ' + t : ''}"><small>${esc(l)}</small><b>${esc(v)}</b></div>`;
  const banner = (s, b) => `<div class="banner"><small>${esc(s)}</small><b>${esc(b)}</b></div>`;
  const chip = (label, on, a, v) => `<button type="button" class="chip${on ? ' on' : ''}" data-a="${a}" data-v="${esc(v === undefined ? '' : v)}">${esc(label)}</button>`;
  const tag = (t, kind) => `<span class="tag ${kind || ''}">${esc(t)}</span>`;
  const kindTag = (k) => tag(kindLabel(k), k === 'BAHAN' ? 'ok' : k === 'KEMASAN' ? 'ac' : 'wr');
  const nav = (title, sub, prev, next) => `<div class="wk"><button class="arr" data-a="${prev}">‹</button><div class="ttl"><h2>${esc(title)}</h2><p>${esc(sub)}</p></div><button class="arr" data-a="${next}">›</button></div>`;
  const field = (label, bind, val, extra) => `<label class="fld"><span>${esc(label)}</span><input class="in" data-bind="${bind}" value="${esc(val)}" ${extra || ''}></label>`;
  const menuBanner = (date) => {
    const mn = C.menuName(D(), date), lib = C.isLibur(D(), date);
    return C.dayLong(date) + ' · ' + (mn || (lib ? 'Libur' : 'Menu belum diisi'));
  };

  const menuBox = (date, label) => {
    const lines = C.menuLines(C.menuRaw(D(), date)); const lib = C.isLibur(D(), date);
    return `<div class="mbox t-menu"><small class="mlab">${esc(label)}</small><div class="mdate">${esc(C.dayLong(date))}</div>${lines.length ? lines.map((x) => `<div class="mn">${esc(x)}</div>`).join('') : `<div class="mn none">${lib ? 'Libur' : 'Menu belum diisi'}</div>`}</div>`;
  };

  // ---------- auth ----------
  function renderAuth(stage) {
    const users = D().users;
    let h = '<div class="auth"><img src="' + LOGO + '" alt="Dapur Sappa" class="biglogo">';
    if (stage === 'code') {
      h += `<h1>Selamat datang</h1><p class="mu">Masukkan kode toko untuk memakai aplikasi</p>
        <input id="code" class="in code" type="password" inputmode="numeric" maxlength="5" autocomplete="off">
        <div id="code-err" class="err" hidden>Kode salah. Coba lagi.</div>
        <button class="btn" data-a="code">Lanjut</button><p class="mu sm">Kode hanya diminta sekali di browser ini.</p>`;
    } else {
      h += `<h1>Kamu siapa?</h1><p class="mu">Pilih nama supaya riwayat tahu siapa yang mencatat.</p><div class="users">`;
      users.forEach((u, i) => { h += `<button class="user" data-a="pick" data-v="${i}"><b>${esc(u.name)}</b><span>Belanja, order, menu dan laporan</span></button>`; });
      h += '</div><button class="btn o" data-a="devlogin">Masuk sebagai developer</button>';
    }
    h += '</div>';
    $('#app').innerHTML = h;
    const c = $('#code'); if (c) { c.focus(); c.addEventListener('keydown', (e) => { if (e.key === 'Enter') A.code(); }); }
  }

  // ---------- shell ----------
  function tabsFor(me) {
    const all = [['menu', 'Menu dan harga'], ['belanja', 'Belanja'], ['order', 'Order'], ['summary', 'Summary'], ['laporan', 'Laporan'], ['brosur', 'Brosur menu'], ['riwayat', 'Riwayat']];
    if (me.isDev) all.push(['kelola', 'Kelola user']);
    return all;
  }
  function shell(me, body) {
    const tabs = tabsFor(me);
    const apk = CFG.GITHUB_USER ? `<a class="lnk" target="_blank" rel="noopener" href="https://github.com/${CFG.GITHUB_USER}/${CFG.GITHUB_REPO}/releases/latest">Unduh APK</a>` : '';
    const online = Store.status === 'Online';
    return `<header class="top"><div class="brand"><img src="${LOGO}" alt=""><b data-a="syncnow" title="${esc(Store.status)}">Dapur Sapp<span class="la">a<i class="dot${online ? ' on' : ''}"></i></span></b></div>
      <div class="right"><span id="clock" class="clk"></span>
      <span class="who">${esc(me.name)}</span>
      <button class="mini" data-a="switch">Ganti user</button></div></header>
      <nav class="tabs">${tabs.map((t) => `<button class="tab${t[0] === 'menu' ? ' hl' : ''}${st.tab === t[0] && !st.route ? ' on' : ''}" data-a="tab" data-v="${t[0]}">${t[1]}</button>`).join('')}${apk}<button class="tsw${isDark() ? ' on' : ''}" data-a="theme" title="Geser atau klik: LIGHT / DARK" aria-label="Tema"><span>${isDark() ? 'DARK' : 'LIGHT'}</span><i></i></button></nav>
      <main>${body}</main><footer>Dapur Sappa web v${CFG.WEB_VERSION} · ${esc(Store.status)}${Store.lastSync ? ' · sinkron ' + C.timeOf(Store.lastSync) : ''}</footer>`;
  }
  function tickClock() {
    const el = $('#clock'); if (!el) return; const n = new Date();
    el.textContent = C.dayShort(today()) + ' · ' + C.timeOf(n.getTime()) + ' WIB';
  }

  // ---------- Belanja (termasuk pengeluaran lain) ----------
  function vBelanja(me) {
    const vd = st.belDate || (st.belDate = today());
    const items = D().purchases.filter((p) => p.date === vd);
    const exps = D().expenses.filter((e) => e.date === vd);
    const forToday = items.filter((p) => p.kind !== 'BULANAN' && (p.useDate || p.date) === p.date).reduce((s, p) => s + p.price, 0);
    const later = items.filter((p) => p.kind === 'BAHAN' && (p.useDate || p.date) > p.date).reduce((s, p) => s + p.price, 0);
    const monthly = items.filter((p) => p.kind === 'BULANAN').reduce((s, p) => s + p.price, 0);
    const expSum = exps.reduce((s, e) => s + e.amount, 0);
    let h = menuBox(today(), 'MENU HARI INI');
    h += `<div class="card t-shop"><h3>Catat belanja dan pengeluaran</h3><p class="mu">Ketik bebas, urutan boleh terbalik. Contoh: ayam dada fillet 36rb 1kg · bensin motor antar 20rb</p>
      <input id="bel-text" class="in" placeholder="selada 3 ikat 9rb" value="${esc(st.f.belText || '')}" autocomplete="off"><div id="bel-prev"></div></div>`;
    h += nav(vd === today() ? 'Belanja hari ini' : 'Belanja', C.dayLong(vd), 'belprev', 'belnext');
    if (!items.length && !exps.length) h += '<p class="mu pad">Belum ada belanja atau pengeluaran di tanggal ini.</p>';
    items.forEach((p) => {
      let sub = (C.num(p.qty) + ' ' + p.unit).trim();
      if (p.kind === 'KEMASAN' && p.qty > 0) sub += ' · ' + C.rp(Math.round(p.price / p.qty)) + ' per ' + (p.unit || 'pcs');
      if (p.kind === 'BAHAN' && (p.useDate || p.date) !== p.date) sub += ' · untuk ' + C.dayName(p.useDate);
      h += `<div class="card t-shop row click" data-a="editpur" data-v="${p.id}"><div><b>${esc(p.name)}</b><div class="mu">${esc(sub)}</div></div><div class="r"><b>${C.rp(p.price)}</b><br>${kindTag(p.kind)}</div></div>`;
    });
    exps.forEach((e) => {
      h += `<div class="card t-spend row"><div><b>${esc(e.name)}</b><div class="mu">Pengeluaran lain · ${esc(userName(e.by))}</div></div><div class="r"><b>${C.rp(e.amount)}</b><br>${e.monthly ? tag('Profit Bulanan', 'wr') : tag('Profit Harian', 'ok')} <button class="link" data-a="spdel" data-v="${e.id}">Hapus</button></div></div>`;
    });
    h += `<div class="card t-shop"><div class="row"><span class="mu">Belanja untuk hari ini</span><b>${C.rp(forToday)}</b></div>${later ? `<div class="row"><span class="mu">Belanja untuk hari lain</span><b>${C.rp(later)}</b></div>` : ''}${expSum ? `<div class="row"><span class="mu">Pengeluaran lain</span><b>${C.rp(expSum)}</b></div>` : ''}${monthly ? `<p class="mu sm">Biaya bulanan ${C.rp(monthly)} masuk ke profit bulanan, tidak ke profit harian.</p>` : ''}</div>`;
    return h;
  }
  function belPreview() {
    const box = $('#bel-prev'); const inp = $('#bel-text'); if (!box || !inp) return;
    const p = C.parse(inp.value); st.f.belText = inp.value;
    if (!p) { box.innerHTML = ''; return; }
    const vd = st.belDate || today();
    if (!p.ok) { box.innerHTML = `<p class="warn">${p.price <= 0 ? 'Harga belum terbaca. Tulis harga seperti 36rb atau 36000.' : 'Nama barang belum terbaca.'}</p><button class="btn o" data-a="belmanual">Isi manual</button>`; return; }
    const kind = st.f.bKind || p.kind;
    const use = st.f.bUse || vd;
    const per = p.qty > 0 ? Math.round(p.price / p.qty) : p.price;
    const perTxt = p.unit === 'kg' ? 'Rp' + C.num(p.price / (p.qty * 1000)) + ' per gram' : C.rp(per) + ' per ' + (p.unit || 'satuan');
    let h = kind === 'LAIN'
      ? `<div class="prev"><small class="okc">Terbaca otomatis</small><b>${esc(p.name)} · ${C.rp(p.price)}</b><div class="mu">Pengeluaran lain (bensin, tambal ban, dll)</div></div>`
      : `<div class="prev"><small class="okc">Terbaca otomatis</small><b>${esc(p.name)} · ${esc((C.num(p.qty) + ' ' + p.unit).trim())} · ${C.rp(p.price)}</b><div class="mu">Jenis: ${esc(kindLabel(kind).toLowerCase())} · ${esc(perTxt)}</div></div>`;
    h += '<div class="mu">Jenis</div><div class="chips">' + ['BAHAN', 'KEMASAN', 'BULANAN', 'LAIN'].map((k) => chip(kindLabel(k), kind === k, 'belkind', k)).join('') + '</div>';
    if (kind === 'BAHAN') {
      h += `<div class="mu">Dipakai untuk tanggal</div><div class="chips">${chip('Hari ini', use === vd, 'beluse', vd)}${chip('Besok', use === C.addDays(vd, 1), 'beluse', C.addDays(vd, 1))}<input type="date" class="in dt" value="${use}" data-chg="beluse"></div>`;
    }
    if (kind === 'LAIN') {
      const ch = st.f.bCharge || 'D';
      h += `<div class="mu">Charge ke</div><div class="chips">${chip('Profit Harian', ch === 'D', 'belcharge', 'D')}${chip('Profit Bulanan', ch === 'M', 'belcharge', 'M')}</div><p class="mu sm">Profit Harian: mengurangi profit hari itu. Profit Bulanan: hanya mengurangi profit bulanan.</p>`;
    }
    h += '<div class="btns"><button class="btn o" data-a="belmanual">Ubah</button><button class="btn" data-a="belsave">Simpan</button></div>';
    box.innerHTML = h;
  }
  function draftPurchase(p, kind, vd, use, me) {
    return { t: 'p', id: C.uuid(), date: vd, useDate: kind === 'BAHAN' ? use : vd, raw: st.f.belText || '', name: p.name, qty: p.qty, unit: p.unit, price: p.price, kind, by: me.idx, del: false };
  }

  // ---------- Order ----------
  function vOrder(me) {
    const date = st.orderDate || (st.orderDate = today());
    const orders = D().orders.filter((o) => o.date === date).slice().sort((a, b) => { const x = C.orderKey(a) + a.customer.toLowerCase(), y = C.orderKey(b) + b.customer.toLowerCase(); return x < y ? -1 : x > y ? 1 : 0; });
    const pax = orders.reduce((s, o) => s + C.orderPortions(o), 0);
    const omzet = orders.reduce((s, o) => s + C.orderTotal(o), 0);
    const unpaid = orders.filter((o) => !o.paid && C.orderTotal(o) > 0);
    let h = '<button class="btn first" data-a="neworder">+ Tambah order</button>';
    h += menuBox(date, date === today() ? 'MENU HARI INI' : 'MENU');
    h += nav('Order', C.dayLong(date), 'ordprev', 'ordnext');
    h += `<div class="g3">${tile('Order', orders.length, false, 't-order')}${tile('Porsi', pax, false, 't-order')}${tile('Omzet', C.rp(omzet), true, 't-money')}</div>`;
    h += '<h3>Daftar order</h3>';
    if (!orders.length) h += '<p class="mu pad">Belum ada order di tanggal ini.</p>';
    orders.forEach((o) => {
      const sub = (o.location ? o.location + ' · ' : '') + (o.only ? 'tanpa porsi' : o.pax + ' pax');
      h += `<div class="card t-order"><div class="row click" data-a="editorder" data-v="${o.id}"><span>${o.time ? `<b class="acc tm">${esc(o.time)}</b> ` : ''}<b>${esc(o.customer)}</b></span><b>${C.rp(C.orderTotal(o))}</b></div><div class="mu">${esc(sub)}</div>${o.remarks ? `<div class="mu">${esc(o.remarks)}</div>` : ''}${(o.extras || []).map((e) => `<div class="mu">Tambahan: ${esc(e.n)} ${e.q} × ${C.rp(e.p)}</div>`).join('')}
        <div class="tags">${C.deliv(o) ? tag(C.deliv(o), 'ac') : ''}${o.only ? tag('Lauk saja', 'ac') : C.extrasTotal(o) > 0 ? tag('+ Tambahan ' + C.rp(C.extrasTotal(o)), 'ac') : ''}${o.paid ? tag('Lunas', 'ok') : tag('Belum bayar', 'wr')}<span class="sp"></span><button class="link" data-a="togglepaid" data-v="${o.id}">${o.paid ? 'Tandai belum' : 'Tandai lunas'}</button></div></div>`;
    });
    if (unpaid.length) h += `<div class="card warnbg"><div class="row"><b>Belum dibayar</b><b>${C.rp(unpaid.reduce((s, o) => s + C.orderTotal(o), 0))}</b></div><div class="mu">${unpaid.length} order belum lunas</div></div>`;
    return h;
  }
  function orderFormInit(id, date) {
    const old = id ? D().orders.find((o) => o.id === id) : null;
    st.f.of = {
      id: old ? old.id : null, date: old ? old.date : date, days: [date], customer: old ? old.customer : '', location: old ? old.location : '',
      pax: old ? old.pax : 1, price: String(old ? old.price : C.defaultPrice(D(), date)), remarks: old ? old.remarks : '',
      only: old ? !!old.only : false, paid: old ? !!old.paid : false, extras: old ? JSON.parse(JSON.stringify(old.extras || [])) : [],
      slot: old ? (old.slot || '') : '', hh: old && old.time ? old.time.split(':')[0] : '', mm: old && old.time ? (old.time.split(':')[1] || '') : '', err: '',
    };
    st.route = { name: 'order' };
  }
  function vOrderForm(me) {
    const f = st.f.of; const price = +f.price || 0; const portions = f.only ? 0 : f.pax;
    const et = f.extras.reduce((s, e) => s + e.q * e.p, 0); const total = portions * price + et;
    const nd = f.id ? 1 : Math.max(1, f.days.length);
    let h = `<div class="back"><button class="link" data-a="back">‹ Kembali</button><h2>${f.id ? 'Edit order' : 'Order baru'}</h2><p class="mu">${esc(menuBanner(f.date))}</p></div><div class="card form t-order">`;
    if (f.id) {
      h += `<label class="fld"><span>Tanggal kirim</span><input type="date" class="in" data-bind="of.date" value="${f.date}"></label>`;
    } else {
      const mon = C.mondayOf(f.date);
      h += `<label class="fld"><span>Pilih minggu (ketuk tanggal mana saja di minggu itu)</span><input type="date" class="in" data-chg="ofdate" value="${f.date}"></label>`;
      h += '<div class="mu">Hari kirim (boleh lebih dari satu)</div><div class="chips">';
      for (let i = 0; i < 7; i++) { const d = C.addDays(mon, i); h += chip(C.DAYS[i].substring(0, 3) + ' ' + C.dayNum(d), f.days.indexOf(d) >= 0, 'ofday', d); }
      h += `${chip('Sen–Jum', false, 'ofpreset', '5')}${chip('Sen–Sab', false, 'ofpreset', '6')}</div><p class="mu sm">${f.days.length} hari dipilih. Satu nama customer, order yang sama dibuat untuk tiap hari.</p>`;
    }
    h += field('Nama customer', 'of.customer', f.customer) + field('Lokasi', 'of.location', f.location, 'placeholder="mis. Blok C3 No.5"');
    h += '<div class="mu">Waktu pengantaran (info untuk driver)</div><div class="slots">' + C.SLOTS.map((s) => chip(s[0], f.slot === s[0], 'ofslot', s[0])).join('') + '</div>';
    h += `<div class="slots"><label class="fld"><span>Jam (0-23)</span><input class="in" style="width:90px" inputmode="numeric" maxlength="2" placeholder="16" data-bind="of.hh" data-num="1" value="${esc(f.hh)}"></label><b>:</b><label class="fld"><span>Menit (0-59)</span><input class="in" style="width:90px" inputmode="numeric" maxlength="2" placeholder="00" data-bind="of.mm" data-num="1" value="${esc(f.mm)}"></label></div>`;
    h += `<div class="mu">Jenis order</div><div class="chips">${chip('Porsi', !f.only, 'ofonly', '0')}${chip('Lauk saja', f.only, 'ofonly', '1')}</div>`;
    if (!f.only) {
      h += `<div class="mu">Jumlah pax</div><div class="step"><button class="arr" data-a="ofpax" data-v="-1">−</button><b>${f.pax}</b><button class="arr" data-a="ofpax" data-v="1">+</button></div>`;
      h += field('Harga per porsi (Rp)', 'of.price', f.price, 'inputmode="numeric" data-num="1"');
    }
    h += `<div class="row"><b>Pesanan tambahan</b><span class="mu">${f.extras.length} item</span></div>`;
    f.extras.forEach((e, i) => { h += `<div class="card inner row"><div><b>${esc(e.n)}</b><div class="mu">${e.q} × ${C.rp(e.p)}${e.c ? ' · ' + esc(e.c) : ''}</div></div><div><b>${C.rp(e.q * e.p)}</b> <button class="link" data-a="ofdelx" data-v="${i}">Hapus</button></div></div>`; });
    h += '<button class="btn o" data-a="ofaddx">+ Tambah pesanan</button>';
    h += field('Remarks', 'of.remarks', f.remarks, 'placeholder="mis. tanpa sambal"');
    h += `<div class="mu">Pembayaran</div><div class="chips">${chip('Lunas', f.paid, 'ofpaid', '1')}${chip('Belum bayar', !f.paid, 'ofpaid', '0')}</div>`;
    h += `<div class="card soft">${f.only ? '' : `<div class="row"><span class="mu">Porsi ${f.pax} × ${C.rp(price)}</span><b>${C.rp(f.pax * price)}</b></div>`}${f.extras.length ? `<div class="row"><span class="mu">Pesanan tambahan (${f.extras.length} item)</span><b>${C.rp(et)}</b></div>` : ''}<div class="row tot"><b>Total per hari</b><b class="acc">${C.rp(total)}</b></div>${nd > 1 ? `<div class="row"><b>Total ${nd} hari</b><b class="acc">${C.rp(total * nd)}</b></div>` : ''}</div>`;
    if (portions > 0) h += `<p class="mu sm">Kemasan ikut terpotong ${portions} pcs per jenis kemasan.</p>`;
    if (f.err) h += `<p class="err">${esc(f.err)}</p>`;
    h += `<div class="btns"><button class="btn" data-a="ofsave">${nd > 1 ? 'Simpan ' + nd + ' order' : 'Simpan order'}</button><button class="btn o" data-a="back">Batal</button>${f.id ? '<button class="link dng" data-a="ofdel">Hapus order</button>' : ''}</div></div>`;
    return h;
  }

  // ---------- Summary / detail ----------
  function dayCard(d) {
    const closed = d.libur && !d.orders.length;
    const ml = C.menuLines(d.menuName);
    const menuHtml = ml.length ? ml.map((x) => `<div class="mn">${esc(x)}</div>`).join('') : `<div class="mn none">${closed ? 'Libur' : 'Menu belum diisi'}</div>${closed ? '<div class="mu sm">Bisa dibuka untuk PO spesial</div>' : ''}`;
    return `<div class="card click t-menu" data-a="detail" data-v="${d.date}"><div class="row start"><div><div class="dt2">${C.dayName(d.date)} · ${C.dm(d.date)}</div>${menuHtml}</div>${closed ? tag('Libur') : tag(d.pax + ' pax', d.pax ? 'ac' : '')}</div>${d.orders.length || d.cost > 0 ? `<div class="row"><span class="mu">Modal ${C.rp(d.cost)}</span><b class="${d.profit >= 0 ? 'okc' : 'acc'}">Profit ${C.rp(d.profit)}</b></div>` : (closed ? '' : '<div class="mu">Belum ada order</div>')}</div>`;
  }
  function vSummary(me) {
    const ws = st.weekStart || (st.weekStart = C.mondayOf(today()));
    const days = C.week(D(), ws); const sum = (f) => days.reduce((s, d) => s + f(d), 0);
    const end = C.addDays(ws, 6);
    let h = nav('Summary Week ' + C.weekNo(ws), C.range(ws, end), 'wkprev', 'wknext');
    h += `<div class="g2">${tile('Total pax', sum((d) => d.pax), false, 't-money')}${tile('Omzet', C.rp(sum((d) => d.revenue)), false, 't-money')}</div>`;
    h += tile('Gross Profit', C.rp(sum((d) => d.profit)), true, 't-money');
    h += '<h3>Menu minggu ini</h3><div class="g2">' + days.map(dayCard).join('') + '</div>';
    return h;
  }
  function vDetail(me) {
    const date = st.route.date; const d = C.day(D(), date);
    let h = `<div class="back"><button class="link" data-a="back">‹ Kembali</button><h2>${C.dayLong(date)}</h2><p class="mu">${esc(C.menuLines(d.menuName).join(' · ') || 'Menu belum diisi')} · ${d.pax} pax · ${d.orders.length} order</p></div>`;
    h += `<div class="g3">${tile('Omzet', C.rp(d.revenue), false, 't-money')}${tile('Modal', C.rp(d.cost), false, 't-money')}${tile('Profit', C.rp(d.profit), true, 't-money')}</div>`;
    if (d.expCost > 0) h += `<p class="mu">Profit sudah dikurangi pengeluaran lain ${C.rp(d.expCost)} (${esc(d.expenses.map((e) => e.name).join(', '))}).</p>`;
    h += '<h3>Bahan dan kemasan</h3><div class="card t-shop"><table><tr><th>Bahan</th><th>Qty</th><th>Harga</th><th>Per porsi</th></tr>';
    if (!d.bahan.length && !d.kemasan.length) h += '<tr><td colspan="4" class="mu">Belum ada belanja untuk hari ini.</td></tr>';
    d.bahan.forEach((p) => { h += `<tr><td>${esc(p.name)}</td><td>${esc((C.num(p.qty) + ' ' + p.unit).trim())}</td><td>${C.th(p.price)}</td><td>${d.pax ? C.th(p.price / d.pax) : '-'}</td></tr>`; });
    d.kemasan.forEach((k) => { h += `<tr><td>${esc(k.name)}</td><td>${k.pcs} pcs</td><td>${C.th(k.cost)}</td><td>${C.th(k.unitPrice)}</td></tr>`; });
    h += `<tr class="tot"><td>Total</td><td></td><td>${C.th(d.cost)}</td><td class="acc">${d.pax ? C.th(d.costPerPax) : '-'}</td></tr></table></div>`;
    if (d.pax) h += `<p class="mu sm">Per porsi = harga ÷ ${d.pax} pax terjual. Biaya bulanan tidak ikut dihitung di sini.</p>`;
    h += '<h3>Order hari ini</h3>';
    if (!d.orders.length) h += '<p class="mu">Belum ada order.</p>';
    d.orders.forEach((o) => { h += `<div class="card t-order row click" data-a="editorder" data-v="${o.id}"><div><b>${esc(o.customer)}</b><div class="mu">${esc((C.deliv(o) ? C.deliv(o) + ' · ' : '') + (o.location ? o.location + ' · ' : '') + (o.only ? 'lauk saja' : o.pax + ' pax') + (C.extrasTotal(o) ? ' · + tambahan ' + C.rp(C.extrasTotal(o)) : ''))}</div></div><b>${C.rp(C.orderTotal(o))}</b></div>`; });
    return h;
  }

  // ---------- Laporan ----------
  function dayRow(d) {
    return `<div class="card row click" data-a="detail" data-v="${d.date}"><div><b>${C.dayShort(d.date)}</b><div class="mu">${esc(C.menuLines(d.menuName).join(' · ') || '-')} · ${d.pax} pax</div></div><div class="r"><b class="${d.profit >= 0 ? 'okc' : 'acc'}">${C.rp(d.profit)}</b><div class="mu">Omzet ${C.rp(d.revenue)} · Modal ${C.rp(d.cost)}</div><div class="mu">Margin ${C.pct(d.profit, d.revenue)}</div></div></div>`;
  }
  function vLaporan() {
    const rm = st.repMonth || (st.repMonth = C.firstOfMonth(today()));
    const rw = st.repWeek || (st.repWeek = C.mondayOf(today()));
    let h = '<button class="btn o" data-a="exportxls">Export laporan ke Excel (bulan, minggu, hari, pengeluaran)</button>';
    h += `<div class="chips">${chip('Harian', st.repTab === 0, 'reptab', 0)}${chip('Mingguan', st.repTab === 1, 'reptab', 1)}${chip('Bulanan', st.repTab === 2, 'reptab', 2)}</div>`;
    const monthNav = () => {
      const t = today(); const cur = rm.substring(0, 7) === t.substring(0, 7);
      return nav(C.monthName(rm), '1 – ' + (cur ? C.dayNum(t) : C.monthLen(rm)) + ' ' + C.monthAbbr(rm) + (cur ? ' (berjalan)' : ''), 'mprev', 'mnext');
    };
    if (st.repTab === 0) {
      const m = C.month(D(), rm); h += monthNav();
      const act = m.days.filter((d) => d.revenue > 0 || d.cost > 0 || d.expCost > 0).reverse();
      h += act.length ? '<div class="g2">' + act.map(dayRow).join('') + '</div>' : '<p class="mu">Belum ada data di bulan ini.</p>';
      h += '<p class="mu sm">Klik hari untuk melihat rincian bahan dan order. Margin = profit ÷ omzet.</p>';
    } else if (st.repTab === 1) {
      const days = C.week(D(), rw); const sum = (f) => days.reduce((s, d) => s + f(d), 0);
      h += nav('Week ' + C.weekNo(rw), C.range(rw, C.addDays(rw, 6)), 'rwprev', 'rwnext');
      h += tile('Gross Profit minggu ini', C.rp(sum((d) => d.profit)), true, 't-money');
      h += `<div class="g4">${tile('Margin', C.pct(sum((d) => d.profit), sum((d) => d.revenue)), false, 't-money')}${tile('Omzet', C.rp(sum((d) => d.revenue)), false, 't-money')}${tile('Modal belanja', C.rp(sum((d) => d.cost)), false, 't-money')}${tile('Total pax', sum((d) => d.pax), false, 't-money')}</div>`;
      h += '<div class="g2">' + days.map(dayRow).join('') + '</div><p class="mu sm">Margin mingguan = gross profit ÷ omzet. Biaya bulanan tidak dihitung di laporan mingguan.</p>';
    } else {
      const m = C.month(D(), rm); h += monthNav();
      const gross = m.profit + m.monthlyCost;
      h += tile('Profit bersih bulan ini', C.rp(m.profit), true, 't-money');
      h += `<div class="g2">${tile('Margin bersih', C.pct(m.profit, m.revenue), false, 't-money')}${tile('Margin sebelum biaya bulanan', C.pct(gross, m.revenue), false, 't-money')}</div>`;
      h += `<div class="g4">${tile('Omzet', C.rp(m.revenue), false, 't-money')}${tile('Modal belanja', C.rp(m.modal), false, 't-money')}${tile('Pengeluaran lain', C.rp(m.expHarian), false, 't-money')}${tile('Biaya bulanan', C.rp(m.monthlyCost), false, 't-money')}</div>`;
      h += '<h3>Omzet dan margin per minggu</h3><div class="card">';
      const groups = {}; m.days.forEach((d) => { const k = C.mondayOf(d.date); (groups[k] = groups[k] || []).push(d); });
      st.f.wk = st.f.wk || {};
      Object.keys(groups).sort().forEach((k) => {
        const l = groups[k]; const rev = l.reduce((s, d) => s + d.revenue, 0); const pr = l.reduce((s, d) => s + d.profit, 0); const open = !!st.f.wk[k];
        h += `<div class="row ln wkrow" data-a="wkopen" data-v="${k}"><div><b>${open ? '▾' : '▸'} Week ${C.weekNo(k)}</b><div class="mu">${C.dayNum(l[0].date)}–${C.dayNum(l[l.length - 1].date)} ${C.monthAbbr(l[0].date)} · ketuk untuk data harian</div></div><div class="r"><b>${C.rp(rev)}</b><div class="mu">Profit ${C.rp(pr)} · Margin ${C.pct(pr, rev)}</div></div></div>`;
        if (open) {
          const act = l.filter((d) => d.revenue > 0 || d.cost > 0 || d.expCost > 0);
          h += '<div class="sub">' + (act.length ? act.map((d) => `<div class="row ln click" data-a="detail" data-v="${d.date}"><div><b>${C.dayShort(d.date)}</b><div class="mu">${esc(C.menuLines(d.menuName).join(' · ') || '-')} · ${d.pax} pax</div></div><div class="r"><b>${C.rp(d.revenue)}</b><div class="mu">Profit ${C.rp(d.profit)} · Margin ${C.pct(d.profit, d.revenue)}</div></div></div>`).join('') : '<p class="mu sm">Belum ada data harian di minggu ini.</p>') + '</div>';
        }
      });
      h += `</div><div class="row"><h3>Biaya bulanan</h3><b>${C.rp(m.monthlyCost)}</b></div><div class="card t-shop">`;
      h += m.monthlyLines.length ? m.monthlyLines.map((l) => `<div class="row ln"><span>${esc(l[0])}</span><b>${C.th(l[1])}</b></div>`).join('') : '<p class="mu">Belum ada biaya bulanan.</p>';
      h += '</div><p class="mu sm">Biaya bulanan hanya dihitung di laporan bulanan.</p>';
      h += `<div class="card warnbg click" data-a="unpaid"><div class="row"><b>Belum dibayar customer</b><b>${C.rp(m.unpaid.reduce((s, o) => s + C.orderTotal(o), 0))}</b></div><div class="mu">${m.unpaid.length} order · klik untuk ${st.f.unpaid ? 'menutup' : 'melihat rincian'}</div>`;
      if (st.f.unpaid) h += m.unpaid.slice().sort((a, b) => (a.date < b.date ? -1 : 1)).map((o) => `<div class="row ln"><span>${esc(o.customer)} · ${C.dayShort(o.date)}</span><b>${C.rp(C.orderTotal(o))}</b></div>`).join('');
      h += '</div>';
    }
    return h;
  }

  // ---------- Menu ----------
  function menuInit(week) {
    const saved = D().menus[week]; const base = saved || { days: ['', '', '', '', '', '', ''], libur: [false, false, false, false, false, false, true], price: C.defaultPrice(D(), week) };
    const lines = [];
    for (let i = 0; i < 7; i++) { const p = C.menuLines(base.days[i]); for (let j = 0; j < 3; j++) lines.push(p[j] || ''); }
    st.f.mn = { week, lines, libur: base.libur.slice(), price: String(base.price), msg: '' };
  }
  function vMenu(me) {
    if (!st.f.mn) menuInit(C.mondayOf(today()));
    const f = st.f.mn; const ed = me.canAdmin;
    let h = nav('Week ' + C.weekNo(f.week), C.range(f.week, C.addDays(f.week, 6)), 'mnprev', 'mnnext');
    h += '<div class="card">';
    for (let i = 0; i < 7; i++) {
      const dt = C.addDays(f.week, i);
      h += `<div class="mrow"><div class="dn"><b>${C.DAYS[i]}</b><div class="mu sm">${C.dm(dt)}</div></div><div class="mlines">${[0, 1, 2].map((j) => `<input class="in" data-bind="mn.l${i * 3 + j}" value="${esc(f.lines[i * 3 + j])}" placeholder="${j === 0 && i === 6 ? 'Menu PO spesial' : ['Nasi / karbo', 'Lauk utama', 'Sayur / pendamping'][j]}" ${ed ? '' : 'disabled'}>`).join('')}</div>${chip('Libur', f.libur[i], ed ? 'mnlibur' : '', i)}</div>`;
    }
    h += '</div>' + banner('Minggu biasanya libur', 'Untuk PO menu spesial, matikan Libur dan isi menunya. Hari itu tampil seperti hari biasa.');
    h += field('Harga jual per porsi (Rp)', 'mn.price', f.price, `inputmode="numeric" data-num="1" ${ed ? '' : 'disabled'}`) + '<p class="mu">Harga bisa diubah per order saat mencatat order.</p>';
    if (!ed) h += '<p class="warn">Hanya admin yang bisa mengubah menu.</p>';
    if (f.msg) h += `<p class="okc">${esc(f.msg)}</p>`;
    if (ed) h += '<div class="btns"><button class="btn" data-a="mnsave">Simpan menu</button><button class="btn o" data-a="mncopy">Salin dari minggu lalu</button></div>';
    return h;
  }

  // ---------- Brosur menu harian ----------
  function vBrosur() {
    const wk = st.brWeek || (st.brWeek = C.mondayOf(today()));
    let h = '<h2>Export brosur menu harian</h2>' + nav('Week ' + C.weekNo(wk), Export.brosurDate(wk), 'brprev', 'brnext');
    h += '<div id="brbox"><p class="mu">Menyiapkan brosur...</p></div>';
    h += '<button class="btn" data-a="brdl">Unduh PNG (1080 × 1920)</button><p class="mu sm">Menu diambil dari Menu mingguan. Hari libur tampil LIBUR.</p><p class="mu sm" id="brfont"></p>';
    setTimeout(() => {
      Export.brosurCanvas(D(), wk, (cv) => {
        const box = $('#brbox'); if (!box) return;
        cv.id = 'brcv'; cv.className = 'brosur'; box.innerHTML = ''; box.appendChild(cv);
        const f = $('#brfont'); if (f && !Export.fontOk()) f.textContent = 'Font LAZYDOG belum terpasang (taruh lazydog.ttf di folder web), memakai font cadangan.';
      });
    }, 0);
    return h;
  }

  // ---------- Riwayat ----------
  function vRiwayat() {
    const filters = ['Semua', 'Belanja', 'Order', 'Pengeluaran', 'Menu', 'User'];
    let h = '<h2>Riwayat perubahan</h2><p class="mu">Siapa mengubah apa, dan kapan</p><div class="chips">' + filters.map((f) => chip(f, st.rf === f, 'rf', f)).join('') + '</div>';
    const list = D().audits.filter((a) => st.rf === 'Semua' || a.cat === st.rf).slice(0, 300);
    if (!list.length) h += '<p class="mu">Belum ada riwayat.</p>';
    let last = null; const t = today(), y = C.addDays(t, -1);
    list.forEach((a) => {
      const d = C.dateOfTs(a.ts);
      if (d !== last) { last = d; h += `<h4>${d === t ? 'Hari ini · ' + C.dayShort(d) : d === y ? 'Kemarin · ' + C.dayShort(d) : C.dayLong(d)}</h4>`; }
      h += `<div class="card"><div class="row"><b>${esc(a.title)}</b><span class="mu">${C.timeOf(a.ts)}</span></div><div class="mu">${esc(a.who)}</div>${a.detail ? `<div class="det">${esc(a.detail)}</div>` : ''}</div>`;
    });
    return h + '<p class="mu sm">Data yang dihapus tetap tercatat di sini.</p>';
  }

  // ---------- Kelola user ----------
  function vKelola() {
    if (!st.f.ku) st.f.ku = { users: D().users.map((u) => ({ name: u.name })), project: Store.project(), key: Store.apiKey(), msg: '' };
    const f = st.f.ku;
    let h = '<h2>Kelola user</h2><p class="mu">Hanya developer. Semua user punya akses penuh (belanja, order, menu, laporan).</p>';
    f.users.forEach((u, i) => { h += `<div class="card form">${field('Nama User ' + (i + 1), 'ku.n' + i, u.name)}</div>`; });
    h += '<h3>Keamanan</h3>';
    h += '<div class="card row"><div><b>Kode toko</b><div class="mu">5 digit · diminta sekali per browser</div></div><b>•••••</b></div>';
    h += '<div class="card form"><b>Akun developer</b><div class="mu">Masuk hanya dengan password</div><button class="btn o" data-a="kupass">Ganti password developer</button></div>';
    h += `<h3>Sinkron online (Firebase)</h3><div class="card form">${field('Project ID', 'ku.project', f.project)}${field('Web API key', 'ku.key', f.key)}<div class="mu">Status: ${esc(Store.status)}</div></div>`;
    if (f.msg) h += `<p class="okc">${esc(f.msg)}</p>`;
    return h + '<button class="btn" data-a="kusave">Simpan perubahan</button>';
  }

  // ---------- modal ----------
  function openModal(html) { const m = $('#modal'); m.innerHTML = `<div class="ovl"><div class="dlg">${html}</div></div>`; st.modal = true; }
  function closeModal() { $('#modal').innerHTML = ''; st.modal = false; st.m = null; }
  function purModal() {
    const m = st.m; const kinds = m.isNew ? ['BAHAN', 'KEMASAN', 'BULANAN', 'LAIN'] : ['BAHAN', 'KEMASAN', 'BULANAN'];
    openModal(`<h3>${m.isNew ? 'Ubah sebelum simpan' : 'Edit belanja'}</h3>${field('Nama barang', 'm.name', m.name)}<div class="two">${field('Jumlah', 'm.qty', m.qty)}${field('Satuan', 'm.unit', m.unit)}</div>${field('Harga total (Rp)', 'm.price', m.price, 'inputmode="numeric" data-num="1"')}
      <div class="mu">Jenis</div><div class="chips">${kinds.map((k) => chip(kindLabel(k), m.kind === k, 'mkind', k)).join('')}</div>
      ${m.kind === 'BAHAN' ? `<label class="fld"><span>Dipakai untuk tanggal</span><input type="date" class="in" data-bind="m.use" value="${m.use}"></label>` : ''}
      ${m.kind === 'LAIN' ? `<div class="mu">Charge ke</div><div class="chips">${chip('Profit Harian', m.charge !== 'M', 'mcharge', 'D')}${chip('Profit Bulanan', m.charge === 'M', 'mcharge', 'M')}</div>` : ''}
      ${m.err ? `<p class="err">${esc(m.err)}</p>` : ''}<div class="btns"><button class="btn" data-a="msave">Simpan</button>${m.isNew ? '' : '<button class="btn o" data-a="mdel">Hapus</button>'}<button class="btn o" data-a="mcancel">Batal</button></div>`);
  }
  function extraModal() {
    const m = st.m;
    openModal(`<h3>Tambah pesanan</h3><div class="mu">Pilih cepat</div><div class="chips">${['Lauk tambahan', 'Nasi tambahan', 'Sambal'].map((n) => chip(n, m.n === n, 'xquick', n)).join('')}${chip('Lainnya', false, 'xquick', '')}</div>
      ${field('Tambah apa', 'm.n', m.n)}<div class="mu">Jumlah</div><div class="step"><button class="arr" data-a="xqty" data-v="-1">−</button><b>${m.q}</b><button class="arr" data-a="xqty" data-v="1">+</button></div>
      ${field('Harga satuan (Rp)', 'm.p', m.p, 'inputmode="numeric" data-num="1"')}${field('Catatan (opsional)', 'm.c', m.c)}
      <div class="row"><span class="mu">Total tambahan</span><b>${C.rp(m.q * (+m.p || 0))}</b></div><p class="mu sm">Masuk ke omzet hari itu.</p>
      ${m.err ? `<p class="err">${esc(m.err)}</p>` : ''}<div class="btns"><button class="btn" data-a="xsave">Simpan pesanan</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
  }
  function devModal(err) {
    openModal(`<h3>Login developer</h3><label class="fld"><span>Password</span><input class="in" type="password" data-bind="m.p" id="devpass"></label>${err ? '<p class="err">Password salah.</p>' : ''}<div class="btns"><button class="btn" data-a="devok">Masuk</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
    st.m = st.m || {};
    const el = $('#devpass'); if (el) el.focus();
  }
  function passModal(err) {
    openModal(`<h3>Ganti password developer</h3><label class="fld"><span>Password baru</span><input class="in" type="password" data-bind="m.p1"></label><label class="fld"><span>Ulangi password</span><input class="in" type="password" data-bind="m.p2"></label>${err ? `<p class="err">${esc(err)}</p>` : ''}<div class="btns"><button class="btn" data-a="passok">Simpan</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
  }

  // ---------- render ----------
  function body(me) {
    if (st.route) {
      if (st.route.name === 'order') return vOrderForm(me);
      if (st.route.name === 'detail') return vDetail(me);
    }
    switch (st.tab) {
      case 'belanja': return vBelanja(me);
      case 'order': return vOrder(me);
      case 'summary': return vSummary(me);
      case 'laporan': return vLaporan(me);
      case 'menu': return vMenu(me);
      case 'brosur': return vBrosur(me);
      case 'riwayat': return vRiwayat(me);
      case 'kelola': return vKelola(me);
    }
    return '';
  }
  function render() {
    if (LS.get('ds_code', '') !== '1') return renderAuth('code');
    const me = getMe();
    if (!me) return renderAuth('pick');
    const tabs = tabsFor(me);
    if (!st.tab || !tabs.some((t) => t[0] === st.tab)) st.tab = 'summary';
    $('#app').innerHTML = shell(me, body(me));
    tickClock();
    if (st.tab === 'belanja' && !st.route) belPreview();
    window.scrollTo(0, 0);
  }
  function softRender() {
    const ae = document.activeElement;
    if (st.modal || (ae && (ae.tagName === 'INPUT' || ae.tagName === 'TEXTAREA' || ae.tagName === 'SELECT'))) {
      const f = $('footer'); if (f) f.textContent = 'Dapur Sappa web v' + CFG.WEB_VERSION + ' · ' + Store.status;
      return;
    }
    const y = window.scrollY; render(); window.scrollTo(0, y);
  }
  function keepRender() { const y = window.scrollY; render(); window.scrollTo(0, y); }

  // ---------- aksi ----------
  const A = {
    code() {
      const v = ($('#code') || {}).value || '';
      if (C.sha256(v) === CFG.CODE_SHA256) { LS.set('ds_code', '1'); render(); } else { const e = $('#code-err'); if (e) e.hidden = false; }
    },
    pick(v) { LS.set('ds_user', String(v)); LS.set('ds_dev', ''); st.tab = null; render(); },
    devlogin() { st.m = {}; devModal(false); },
    devok() {
      const m = st.m || {};
      if (C.sha256(m.p || '') === devHash()) { LS.set('ds_dev', '1'); closeModal(); st.tab = null; render(); } else devModal(true);
    },
    switch() { LS.set('ds_user', '-1'); LS.set('ds_dev', ''); st.tab = null; st.route = null; render(); },
    syncnow() { Store.kick(); },
    exportxls() { try { Export.downloadReport(D()); } catch (e) { alert('Gagal export: ' + e.message); } },
    brprev() { st.brWeek = C.addDays(st.brWeek, -7); render(); }, brnext() { st.brWeek = C.addDays(st.brWeek, 7); render(); },
    brdl() { const cv = $('#brcv'); if (cv) Export.downloadBrosur(cv, st.brWeek); },
    theme() { LS.set('ds_theme', isDark() ? 'light' : 'dark'); applyTheme(); keepRender(); },
    tab(v) { st.tab = v; st.route = null; if (v === 'menu') menuInit(C.mondayOf(today())); if (v === 'kelola') st.f.ku = null; render(); },
    back() { st.route = null; keepRender(); },
    detail(v) { st.route = { name: 'detail', date: v }; render(); },
    // belanja
    belprev() { st.belDate = C.addDays(st.belDate, -1); render(); }, belnext() { st.belDate = C.addDays(st.belDate, 1); render(); },
    belkind(v) { st.f.bKind = v; belPreview(); },
    belcharge(v) { st.f.bCharge = v; belPreview(); },
    mcharge(v) { st.m.charge = v; purModal(); },
    beluse(v, el) { st.f.bUse = (el && el.value) || v; belPreview(); },
    belmanual() {
      const me = getMe(); const p = C.parse((st.f.belText || '')) || { name: '', qty: 1, unit: '', price: 0, kind: 'BAHAN' }; const vd = st.belDate;
      const kind = st.f.bKind || p.kind;
      st.m = { isNew: true, old: null, id: C.uuid(), date: vd, name: p.name, qty: C.num(p.qty), unit: p.unit, price: String(p.price || ''), kind, use: st.f.bUse || vd, raw: st.f.belText || '', charge: st.f.bCharge || 'D', err: '' };
      purModal();
    },
    belsave() {
      const me = getMe(); const p = C.parse(st.f.belText || ''); if (!p || !p.ok) return;
      const vd = st.belDate; const kind = st.f.bKind || p.kind;
      if (kind === 'LAIN') saveExpense({ t: 'e', id: C.uuid(), date: vd, name: p.name, amount: p.price, monthly: st.f.bCharge === 'M', by: me.idx, del: false }, me);
      else savePurchase(draftPurchase(p, kind, vd, st.f.bUse || vd, me), null, me);
      st.f.belText = ''; st.f.bKind = null; st.f.bUse = null; st.f.bCharge = null; keepRender();
    },
    editpur(v) {
      const p = D().purchases.find((x) => x.id === v); if (!p) return;
      st.m = { isNew: false, old: p, id: p.id, date: p.date, name: p.name, qty: C.num(p.qty), unit: p.unit, price: String(p.price), kind: p.kind, use: p.useDate || p.date, raw: p.raw, err: '' };
      purModal();
    },
    mkind(v) { st.m.kind = v; purModal(); },
    msave() {
      const m = st.m, me = getMe(); const price = +m.price || 0;
      if (!m.name.trim() || price <= 0) { m.err = 'Nama dan harga harus diisi.'; purModal(); return; }
      if (m.kind === 'LAIN') {
        saveExpense({ t: 'e', id: m.id, date: m.date, name: m.name.trim(), amount: price, monthly: m.charge === 'M', by: me.idx, del: false }, me);
        st.f.belText = ''; st.f.bKind = null; st.f.bUse = null; st.f.bCharge = null; closeModal(); keepRender(); return;
      }
      const rec = { t: 'p', id: m.id, date: m.date, useDate: m.kind === 'BAHAN' ? m.use : m.date, raw: m.raw || '', name: m.name.trim(), qty: parseFloat(String(m.qty).replace(',', '.')) || 1, unit: (m.unit || '').trim(), price, kind: m.kind, by: m.old ? m.old.by : me.idx, del: false };
      savePurchase(rec, m.old, me);
      if (m.isNew) { st.f.belText = ''; st.f.bKind = null; st.f.bUse = null; }
      closeModal(); keepRender();
    },
    mdel() { deletePurchase(st.m.old, getMe()); closeModal(); keepRender(); },
    mcancel() { closeModal(); },
    // order
    ordprev() { st.orderDate = C.addDays(st.orderDate, -1); render(); }, ordnext() { st.orderDate = C.addDays(st.orderDate, 1); render(); },
    neworder() { orderFormInit(null, st.orderDate); render(); },
    editorder(v) { const o = D().orders.find((x) => x.id === v); if (o) { orderFormInit(v, o.date); render(); } },
    togglepaid(v) { const o = D().orders.find((x) => x.id === v); if (o) { saveOrder(Object.assign({}, o, { paid: !o.paid }), o, getMe()); keepRender(); } },
    ofonly(v) { st.f.of.only = v === '1'; keepRender(); },
    ofpaid(v) { st.f.of.paid = v === '1'; keepRender(); },
    ofdate(v) { const f = st.f.of; if (!v) return; f.date = v; f.days = [v]; keepRender(); },
    ofday(v) { const f = st.f.of; const i = f.days.indexOf(v); if (i >= 0) f.days.splice(i, 1); else f.days.push(v); f.days.sort(); if (f.days.length) f.date = f.days[0]; keepRender(); },
    ofpreset(v) { const f = st.f.of; const mon = C.mondayOf(f.date); f.days = []; for (let i = 0; i < +v; i++) f.days.push(C.addDays(mon, i)); f.date = f.days[0]; keepRender(); },
    ofslot(v) { const f = st.f.of; if (f.slot === v) { f.slot = ''; f.hh = ''; f.mm = ''; } else { f.slot = v; const t = C.slotTime(v).split(':'); f.hh = t[0]; f.mm = t[1]; } keepRender(); },
    wkopen(v) { st.f.wk = st.f.wk || {}; st.f.wk[v] = !st.f.wk[v]; keepRender(); },
    ofpax(v) { st.f.of.pax = Math.max(1, st.f.of.pax + +v); keepRender(); },
    ofdelx(v) { st.f.of.extras.splice(+v, 1); keepRender(); },
    ofaddx() { st.m = { n: '', q: 1, p: '', c: '', err: '' }; extraModal(); },
    xquick(v) { st.m.n = v; extraModal(); },
    xqty(v) { st.m.q = Math.max(1, st.m.q + +v); extraModal(); },
    xsave() {
      const m = st.m; const p = +m.p || 0;
      if (!m.n.trim() || p <= 0) { m.err = 'Nama dan harga harus diisi.'; extraModal(); return; }
      st.f.of.extras.push({ n: m.n.trim(), q: m.q, p, c: (m.c || '').trim() }); closeModal(); keepRender();
    },
    ofsave() {
      const f = st.f.of, me = getMe(); const price = +f.price || 0;
      const hv = f.hh === '' ? null : +f.hh, mv = f.mm === '' ? 0 : +f.mm; const hasT = f.hh !== '' || f.mm !== '';
      const badT = hasT && (hv === null || isNaN(hv) || isNaN(mv) || hv < 0 || hv > 23 || mv < 0 || mv > 59);
      const tstr = hasT && !badT ? String(hv).padStart(2, '0') + ':' + String(mv).padStart(2, '0') : '';
      if (tstr && !f.slot) f.slot = hv < 11 ? 'PAGI' : hv < 15 ? 'SIANG' : hv < 18 ? 'SORE' : 'MALAM';
      if (badT) f.err = 'Jam atau menit tidak valid. Contoh: jam 16, menit 00';
      else if (!f.customer.trim()) f.err = 'Nama customer harus diisi.';
      else if (!f.only && price <= 0) f.err = 'Harga per porsi harus diisi.';
      else if (f.only && !f.extras.length) f.err = 'Lauk saja butuh minimal 1 pesanan tambahan.';
      else if (!f.id && !f.days.length) f.err = 'Pilih minimal 1 hari kirim.';
      else {
        const old = f.id ? D().orders.find((o) => o.id === f.id) : null;
        const mk = (id, date) => ({ t: 'o', id, date, customer: f.customer.trim(), location: f.location.trim(), pax: f.only ? 0 : f.pax, remarks: f.remarks.trim(), price, extras: JSON.parse(JSON.stringify(f.extras)), paid: f.paid, only: f.only, slot: f.slot || '', time: tstr, by: old ? old.by : me.idx, del: false });
        if (f.id) { saveOrder(mk(f.id, f.date), old, me); st.orderDate = f.date; }
        else { const list = f.days.slice().sort().map((d) => mk(C.uuid(), d)); saveOrders(list, me); st.orderDate = list[0].date; }
        st.route = null; render(); return;
      }
      keepRender();
    },
    ofdel() { const o = D().orders.find((x) => x.id === st.f.of.id); if (o && confirm('Hapus order ' + o.customer + '? Tetap tercatat di Riwayat.')) { deleteOrder(o, getMe()); st.route = null; render(); } },
    // summary / laporan
    wkprev() { st.weekStart = C.addDays(st.weekStart, -7); render(); }, wknext() { st.weekStart = C.addDays(st.weekStart, 7); render(); },
    reptab(v) { st.repTab = +v; render(); },
    mprev() { st.repMonth = C.addMonths(st.repMonth, -1); render(); }, mnext() { st.repMonth = C.addMonths(st.repMonth, 1); render(); },
    rwprev() { st.repWeek = C.addDays(st.repWeek, -7); render(); }, rwnext() { st.repWeek = C.addDays(st.repWeek, 7); render(); },
    unpaid() { st.f.unpaid = !st.f.unpaid; keepRender(); },
    // menu
    mnprev() { menuInit(C.addDays(st.f.mn.week, -7)); render(); }, mnnext() { menuInit(C.addDays(st.f.mn.week, 7)); render(); },
    mnlibur(v) { if (v === '') return; st.f.mn.libur[+v] = !st.f.mn.libur[+v]; keepRender(); },
    mnsave() { const f = st.f.mn; saveMenu({ week: f.week, days: [0, 1, 2, 3, 4, 5, 6].map((i) => [0, 1, 2].map((j) => f.lines[i * 3 + j].trim()).filter((x) => x).join('\n')), libur: f.libur.slice(), price: +f.price || 20000 }, D().menus[f.week], getMe()); f.msg = 'Menu tersimpan.'; keepRender(); },
    mncopy() {
      const f = st.f.mn; const prev = D().menus[C.addDays(f.week, -7)];
      if (prev) { for (let i = 0; i < 7; i++) { const p = C.menuLines(prev.days[i]); for (let j = 0; j < 3; j++) f.lines[i * 3 + j] = p[j] || ''; } f.libur = prev.libur.slice(); f.price = String(prev.price); f.msg = 'Disalin. Tekan Simpan menu untuk menyimpan.'; } else f.msg = 'Minggu lalu belum ada menu.';
      keepRender();
    },
    spdel(v) { const e = D().expenses.find((x) => x.id === v); if (e) { deleteExpense(e, getMe()); keepRender(); } },
    rf(v) { st.rf = v; render(); },
    // kelola
    kupass() { st.m = {}; passModal(''); },
    passok() {
      const m = st.m;
      if ((m.p1 || '').length < 6) return passModal('Minimal 6 karakter.');
      if (m.p1 !== m.p2) return passModal('Password tidak sama.');
      LS.set('ds_devhash', C.sha256(m.p1)); closeModal(); st.f.ku.msg = 'Password developer diganti (berlaku di browser ini).'; keepRender();
    },
    kusave() {
      const f = st.f.ku, me = getMe();
      const users = f.users.map((u, i) => ({ name: (u.name || '').trim() || 'User ' + (i + 1), role: 'User' }));
      if (JSON.stringify(users) !== JSON.stringify(D().users)) saveUsers(users, me);
      LS.set('ds_project', f.project.trim()); LS.set('ds_key', f.key.trim());
      f.msg = 'Tersimpan.'; Store.kick(); keepRender();
    },
  };

  function bindValue(key, val, el) {
    const parts = key.split('.'); const ns = parts[0], k = parts[1];
    if (el && el.getAttribute('data-num')) { val = digits(val); if (el.value !== val) el.value = val; }
    if (ns === 'of') st.f.of[k] = val;
    else if (ns === 'm') st.m[k] = val;
    else if (ns === 'mn') { if (/^l\d+$/.test(k)) st.f.mn.lines[+k.substring(1)] = val; else st.f.mn[k] = val; }
    else if (ns === 'ku') { if (/^n\d+$/.test(k)) st.f.ku.users[+k.substring(1)].name = val; else st.f.ku[k] = val; }
    if (ns === 'of' && (k === 'price')) { /* total diperbarui saat render berikutnya */ }
  }

  document.addEventListener('click', (e) => {
    if (window.__swiped) { window.__swiped = false; return; }
    const el = e.target.closest('[data-a]'); if (!el) return;
    const a = el.getAttribute('data-a'); if (!a || !A[a]) return;
    if (el.tagName === 'A') e.preventDefault();
    A[a](el.getAttribute('data-v'), el);
  });
  document.addEventListener('input', (e) => {
    const t = e.target;
    if (t.id === 'bel-text') { st.f.bKind = null; belPreview(); return; }
    const b = t.getAttribute && t.getAttribute('data-bind'); if (b) bindValue(b, t.value, t);
  });
  document.addEventListener('change', (e) => {
    const t = e.target; const c = t.getAttribute && t.getAttribute('data-chg');
    if (c && A[c]) { A[c](t.value, t); return; }
    const b = t.getAttribute && t.getAttribute('data-bind'); if (b) bindValue(b, t.value, t);
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.target.id === 'devpass') A.devok(); });

  function applyTheme() {
    const t = LS.get('ds_theme', '');
    if (t) document.documentElement.setAttribute('data-theme', t); else document.documentElement.removeAttribute('data-theme');
  }

  // ---------- mulai ----------
  Store.load();
  Store.onChange = (changed) => { if (changed) softRender(); else { const dt = $('.brand .dot'); if (dt) dt.classList.toggle('on', Store.status === 'Online'); const f = $('footer'); if (f) f.textContent = 'Dapur Sappa web v' + CFG.WEB_VERSION + ' · ' + Store.status + (Store.lastSync ? ' · sinkron ' + C.timeOf(Store.lastSync) : ''); } };
  applyTheme();
  render();
  (() => { // geser tombol tema (swipe) atau klik
    let sx = null;
    document.addEventListener('pointerdown', (e) => { sx = e.target.closest && e.target.closest('.tsw') ? e.clientX : null; });
    document.addEventListener('pointerup', (e) => {
      if (sx === null) return; const dx = e.clientX - sx; sx = null;
      if (Math.abs(dx) > 14) { e.preventDefault(); window.__swiped = true; const want = dx > 0 ? 'dark' : 'light'; if ((want === 'dark') !== isDark()) A.theme(); setTimeout(() => { window.__swiped = false; }, 50); }
    });
  })();
  setInterval(tickClock, 30000);
  Store.start();
  window.__ds = { st, A, Store, render }; // untuk pengujian
})();

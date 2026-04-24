/* global React */
const { useState } = React;

// =============== Icons (inline Lucide-style) ===============
const Icon = ({ name, size = 20, stroke = 'currentColor', strokeWidth = 1.6 }) => {
  const paths = {
    home: <><path d="M3 11 L12 3 L21 11"/><path d="M5 10 V20 H19 V10"/></>,
    send: <><path d="M3 12h10l-3 -3 M3 12l7 3"/><path d="M14 5l7 7l-7 7"/></>,
    activity: <><path d="M3 12h4l3 -8l4 16l3 -8h4"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21c0 -4 4 -7 8 -7s8 3 8 7"/></>,
    arrow_lr: <><path d="M17 7l4 4l-4 4"/><path d="M3 11h18"/><path d="M7 17l-4 -4"/><path d="M21 13H3"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    download: <><path d="M12 4v12"/><path d="M8 12l4 4l4 -4"/><path d="M4 20h16"/></>,
    bell: <><path d="M6 9a6 6 0 0 1 12 0c0 6 3 7 3 7H3s3 -1 3 -7"/><path d="M10 21a2 2 0 0 0 4 0"/></>,
    chevron: <><path d="M9 6l6 6l-6 6"/></>,
    back: <><path d="M15 6l-6 6l6 6"/></>,
    check: <path d="M5 12l5 5l9 -10" strokeWidth="3"/>,
    close: <><path d="M6 6l12 12M18 6L6 18"/></>,
    lock: <><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    zap: <path d="M13 3l-9 12h7l-1 6l9 -12h-7z"/>,
    shield: <><path d="M12 3L4 6v6c0 5 4 8 8 9c4 -1 8 -4 8 -9V6z"/><path d="M9 12l2 2l4 -4"/></>,
    copy: <><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M4 16V6a2 2 0 0 1 2 -2h10"/></>,
    phone: <path d="M5 4h4l2 5l-3 2a11 11 0 0 0 5 5l2 -3l5 2v4a2 2 0 0 1 -2 2A15 15 0 0 1 3 6a2 2 0 0 1 2 -2z"/>,
    search: <><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.5 -4.5"/></>,
    google: <>
      <path d="M22 12.2c0 -.8 -.1 -1.4 -.2 -2H12v4h5.6c-.2 1.3 -1 2.5 -2.2 3.2l3.5 2.7c2 -1.9 3.1 -4.6 3.1 -7.9z" fill="#4285F4" stroke="none"/>
      <path d="M12 22c3 0 5.5 -1 7.4 -2.7l-3.5 -2.7c-1 .7 -2.2 1.1 -3.9 1.1c-3 0 -5.5 -2 -6.4 -4.7l-3.6 2.8A10 10 0 0 0 12 22z" fill="#34A853" stroke="none"/>
      <path d="M5.6 13a6 6 0 0 1 0 -4L2 6.2a10 10 0 0 0 0 11.6L5.6 13z" fill="#FBBC05" stroke="none"/>
      <path d="M12 5.4c1.6 0 3.1 .6 4.2 1.7l3.1 -3.1A10 10 0 0 0 2 6.2l3.6 2.8c.9 -2.7 3.4 -4.6 6.4 -4.6z" fill="#EA4335" stroke="none"/>
    </>
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
      {paths[name]}
    </svg>
  );
};

// Shared gradient def for active tab icons
const GradDefs = () => (
  <svg width="0" height="0" style={{position:'absolute'}}>
    <defs>
      <linearGradient id="sp-tab-grad" x1="0" x2="1" y1="0" y2="0">
        <stop offset="0%" stopColor="#00FFA3"/>
        <stop offset="50%" stopColor="#9945FF"/>
        <stop offset="100%" stopColor="#DC1FFF"/>
      </linearGradient>
    </defs>
  </svg>
);

// =============== Auth / Sign in ===============
const AuthScreen = ({ onSignIn }) => {
  const feed = [
    { from: 'US', to: 'IN', amt: '$200', recv: '₹16,900', t: '12s ago' },
    { from: 'US', to: 'IN', amt: '$75',  recv: '₹6,337', t: '1m ago' },
    { from: 'US', to: 'IN', amt: '$500', recv: '₹42,250', t: '3m ago' },
    { from: 'US', to: 'IN', amt: '$120', recv: '₹10,140', t: '5m ago' },
    { from: 'US', to: 'IN', amt: '$350', recv: '₹29,575', t: '8m ago' },
  ];
  return (
    <div className="sp-auth" style={{justifyContent:'flex-end',padding:'0 0 28px'}}>
      <div className="glow"></div>
      <div className="glow2"></div>

      {/* Top branding section */}
      <div style={{position:'relative',zIndex:2,padding:'48px 28px 0',flex:1,display:'flex',flexDirection:'column',justifyContent:'flex-start'}}>
        <div className="mark" style={{marginBottom:24}}><div className="s">S</div></div>
        <div style={{fontSize:11,letterSpacing:'.14em',textTransform:'uppercase',color:'var(--accent)',fontFamily:'var(--font-mono)',marginBottom:12}}>stablepay</div>
        <h1 style={{fontSize:34,lineHeight:1.1,letterSpacing:'-0.03em',margin:'0 0 12px',fontWeight:700}}>
          Send money home.<br/><span className="pay">Under a minute.</span>
        </h1>
        <div style={{fontSize:14,color:'var(--fg-2)',lineHeight:1.5,marginBottom:24}}>
          Real-time remittances on Solana.<br/>No app needed for your family.
        </div>

        {/* Live feed */}
        <div style={{fontSize:10,letterSpacing:'.12em',textTransform:'uppercase',color:'var(--fg-3)',fontFamily:'var(--font-mono)',marginBottom:10,display:'flex',alignItems:'center',gap:6}}>
          <span style={{width:5,height:5,borderRadius:'50%',background:'var(--success)',boxShadow:'0 0 6px var(--success)',display:'inline-block'}}></span>
          Live transfers
        </div>
        <div style={{display:'flex',flexDirection:'column',gap:6,marginBottom:8}}>
          {feed.map((f,i)=>(
            <div key={i} style={{display:'flex',alignItems:'center',justifyContent:'space-between',padding:'10px 14px',background:'rgba(255,255,255,0.04)',border:'1px solid rgba(255,255,255,0.07)',borderRadius:10,opacity: 1 - i*0.15}}>
              <div style={{display:'flex',alignItems:'center',gap:8}}>
                <span style={{fontSize:14}}>🇺🇸</span>
                <span style={{color:'var(--fg-3)',fontSize:12}}>→</span>
                <span style={{fontSize:14}}>🇮🇳</span>
                <span style={{fontFamily:'var(--font-mono)',fontWeight:600,fontSize:13,color:'var(--fg-1)'}}>{f.amt}</span>
              </div>
              <div style={{textAlign:'right'}}>
                <div style={{fontFamily:'var(--font-mono)',fontSize:12,color:'#86EFAC',fontWeight:600}}>{f.recv}</div>
                <div style={{fontSize:10,color:'var(--fg-3)',marginTop:1}}>{f.t}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Bottom CTA */}
      <div style={{position:'relative',zIndex:2,padding:'0 24px',display:'flex',flexDirection:'column',gap:10}}>
        <button className="sp-btn primary google" onClick={onSignIn}>
          <Icon name="google" size={20} stroke="none"/> Continue with Google
        </button>
        <div style={{fontSize:11,color:'var(--fg-3)',textAlign:'center',fontFamily:'var(--font-mono)'}}>By continuing you agree to StablePay's Terms &amp; Privacy</div>
      </div>
    </div>
  );
};

// =============== Top bar ===============
const TopBar = ({ title, onBack, right }) => (
  <div className="sp-topbar">
    {onBack
      ? <button className="iconbtn" onClick={onBack}><Icon name="back" size={18}/></button>
      : <button className="iconbtn"><Icon name="user" size={18}/></button>
    }
    <div className="title">{title}</div>
    {right || <button className="iconbtn"><Icon name="bell" size={18}/></button>}
  </div>
);

// =============== Bottom tabs ===============
const TabBar = ({ active, onChange }) => (
  <div className="sp-tabs">
    {[
      ['home','Home','home'],
      ['send','Send','arrow_lr'],
      ['activity','Activity','activity'],
      ['me','Me','user'],
    ].map(([k, lbl, ic]) => (
      <div key={k} className={'tab' + (active === k ? ' active' : '')} onClick={() => onChange(k)}>
        <Icon name={ic} size={22}/>
        <span>{lbl}</span>
      </div>
    ))}
  </div>
);

// =============== Home ===============
const HomeScreen = ({ onSend, onOpen, onAdd }) => {
  const txns = [
    { name: 'Raj Patel', sub: '+91 98765 43210 · 2m ago', amt: '100.00 USDC', status: 'esc', statusLbl: 'Escrowed'},
    { name: 'Meera Iyer', sub: '+91 99887 66554 · yesterday', amt: '250.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
    { name: 'Vikram Shah', sub: '+91 99001 23456 · 3 days ago', amt: '75.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
  ];
  return (
    <>
      <TopBar title="stablepay"/>
      <div className="sp-scroll">
        <div className="sp-balance-card">
          <div className="glow"></div>
          <div className="eyebrow">USDC balance · Solana</div>
          <div className="amt"><small>$</small>248.50</div>
          <div className="sub">CrsMd…DAd18 · Available to send</div>
        </div>
        <div className="sp-actions">
          <div className="action primary" onClick={onSend}>
            <div className="ic"><Icon name="arrow_lr" size={18}/></div>
            <div className="lbl">Send</div>
          </div>
          <div className="action" onClick={onAdd}>
            <div className="ic"><Icon name="plus" size={18}/></div>
            <div className="lbl">Add funds</div>
          </div>
        </div>
        <div className="sp-section-h"><h3>Recent</h3><a>See all</a></div>
        {txns.map((t, i) => (
          <div key={i} className="sp-row" onClick={() => onOpen(t)}>
            <div className="av">{t.name.split(' ').map(n=>n[0]).join('')}</div>
            <div className="mid">
              <div className="name">{t.name}</div>
              <div className="sub">{t.sub}</div>
            </div>
            <div className="right">
              <div className="amt">-{t.amt}</div>
              <div className="s" style={{color: t.status==='esc' ? '#D6B4FF' : '#86EFAC'}}>{t.statusLbl}</div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
};

// =============== Send (amount) ===============
const SendAmount = ({ onBack, onNext, amount, setAmount }) => {
  const inr = (parseFloat(amount || 0) * 84.50).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
  return (
    <>
      <TopBar title="Send" onBack={onBack} right={<span style={{fontSize:11,color:'var(--fg-3)',letterSpacing:'.14em',textTransform:'uppercase'}}>STEP 1 OF 3</span>}/>
      <div className="sp-scroll">
        <div className="sp-amount-input">
          <div className="eyebrow">You send</div>
          <div style={{display:'flex',alignItems:'baseline',justifyContent:'center',gap:'4px'}}>
            <small style={{color:'var(--fg-3)',fontFamily:'var(--font-mono)',fontSize:'40px'}}>$</small>
            <input className="field" value={amount} onChange={e => setAmount(e.target.value.replace(/[^\d.]/g,''))} style={{maxWidth: (amount.length + 1) + 'ch'}}/>
          </div>
          <div className="conv">
            They receive <span style={{color:'var(--fg-1)',fontWeight:600}}>₹{inr}</span>
            <div className="rate">Rate locked · 84.50 INR / USD · from open.er-api.com</div>
          </div>
        </div>
        <div className="sp-kv">
          <div className="r"><span className="k">Network fee</span><span className="v" style={{color:'#86EFAC'}}>$0.002</span></div>
          <div className="r"><span className="k">Settlement</span><span className="v">~30 sec</span></div>
          <div className="r"><span className="k">Corridor</span><span className="v">USD → INR</span></div>
        </div>
        <button className="sp-btn primary" onClick={onNext} disabled={!amount || parseFloat(amount) <= 0}>
          Continue <Icon name="chevron" size={16} stroke="#0B1020"/>
        </button>
      </div>
    </>
  );
};

// =============== Send (recipient) ===============
const SendRecipient = ({ onBack, onNext, phone, setPhone }) => {
  const contacts = [
    ['Raj Patel', '+91 98765 43210'],
    ['Meera Iyer', '+91 99887 66554'],
    ['Vikram Shah', '+91 99001 23456'],
    ['Ananya Rao', '+91 90000 12345'],
  ];
  return (
    <>
      <TopBar title="Recipient" onBack={onBack} right={<span style={{fontSize:11,color:'var(--fg-3)',letterSpacing:'.14em',textTransform:'uppercase'}}>STEP 2 OF 3</span>}/>
      <div className="sp-scroll">
        <div style={{position:'relative',marginBottom:16}}>
          <input
            className=""
            placeholder="+91 98765 43210"
            value={phone}
            onChange={e=>setPhone(e.target.value)}
            style={{width:'100%',background:'var(--surface-2)',border:'1px solid var(--border-2)',color:'var(--fg-1)',padding:'14px 14px 14px 42px',borderRadius:'12px',fontSize:'15px',outline:'none',fontFamily:'var(--font-mono)'}}
          />
          <div style={{position:'absolute',left:14,top:14,color:'var(--fg-3)'}}><Icon name="phone" size={16}/></div>
        </div>
        <div style={{fontSize:12,color:'var(--fg-3)',letterSpacing:'.14em',textTransform:'uppercase',margin:'16px 2px 10px'}}>Recent</div>
        {contacts.map(([name, p], i) => (
          <div key={i} className="sp-row" onClick={() => { setPhone(p); onNext(); }}>
            <div className="av">{name.split(' ').map(n=>n[0]).join('')}</div>
            <div className="mid">
              <div className="name">{name}</div>
              <div className="sub">{p}</div>
            </div>
            <Icon name="chevron" size={14} stroke="var(--fg-3)"/>
          </div>
        ))}
      </div>
      <div style={{padding: '12px 20px 16px'}}>
        <button className="sp-btn primary" onClick={onNext} disabled={!phone}>Continue</button>
      </div>
    </>
  );
};

// =============== Send (review + send) ===============
const SendReview = ({ onBack, onSend, amount, phone, sending }) => {
  const inr = (parseFloat(amount || 0) * 84.50).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
  return (
    <>
      <TopBar title="Review" onBack={onBack} right={<span style={{fontSize:11,color:'var(--fg-3)',letterSpacing:'.14em',textTransform:'uppercase'}}>STEP 3 OF 3</span>}/>
      <div className="sp-scroll">
        <div className="sp-detail-top">
          <div className="eyebrow">They receive</div>
          <div className="amt"><small>₹</small>{inr}</div>
          <div className="to">to {phone}</div>
        </div>
        <div className="sp-kv">
          <div className="r"><span className="k">You send</span><span className="v">${parseFloat(amount || 0).toFixed(2)} USDC</span></div>
          <div className="r"><span className="k">FX rate</span><span className="v">84.50 INR / USD</span></div>
          <div className="r"><span className="k">Network fee</span><span className="v" style={{color:'#86EFAC'}}>$0.002</span></div>
          <div className="r"><span className="k">Delivery</span><span className="v">Instant on-chain + UPI</span></div>
          <div className="r"><span className="k">Claim expires</span><span className="v">48 hours</span></div>
        </div>
        <div style={{display:'flex',gap:10,padding:'12px 14px',background:'var(--accent-soft)',border:'1px solid var(--accent-border)',borderRadius:12,marginBottom:16}}>
          <div style={{color:'var(--accent)',flexShrink:0}}><Icon name="shield" size={18}/></div>
          <div style={{fontSize:12,color:'#D6B4FF',lineHeight:1.45}}>
            Funds are held in a Solana escrow until the recipient enters their UPI ID. If unclaimed within 48h, you get an automatic refund.
          </div>
        </div>
      </div>
      <div style={{padding: '12px 20px 16px'}}>
        <button className="sp-btn primary" onClick={onSend} disabled={sending}>
          {sending ? 'Signing…' : <>Confirm &amp; send ${parseFloat(amount || 0).toFixed(2)}</>}
        </button>
      </div>
    </>
  );
};

// =============== Sending → Sent ===============
const SendingScreen = ({ amount, phone, onDone }) => {
  const { useState: us, useEffect: ue } = React;
  const [step, setStep] = us(0);
  const inr = (parseFloat(amount || 0) * 84.50).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2});
  const s = (n) => step > n ? 'done' : step === n ? 'live' : 'pending';
  const complete = step >= 3;

  ue(() => {
    const t1 = setTimeout(() => setStep(1), 900);
    const t2 = setTimeout(() => setStep(2), 1800);
    const t3 = setTimeout(() => setStep(3), 2700);
    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); };
  }, []);

  return (
    <>
      <TopBar title={complete ? 'Transfer sent' : 'Sending…'}/>
      <div className="sp-scroll">
        <div className="sp-detail-top">
          <div className="eyebrow">{complete ? 'Sent' : 'Sending'}</div>
          <div className="amt"><small>$</small>{parseFloat(amount || 0).toFixed(2)}</div>
          <div className="to">to {phone} · ₹{inr}</div>
        </div>
        <div className="sp-kv" style={{padding:'16px'}}>
          <div className="sp-timeline">
            <div className={'step ' + s(0)}>
              <div className="dot">{step > 0 && <Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/>}</div>
              <div className="body"><div className="t">Authorising transfer</div><div className="sub">Securely signing your transaction</div></div>
            </div>
            <div className={'step ' + s(1)}>
              <div className="dot">{step > 1 && <Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/>}</div>
              <div className="body"><div className="t">Locking funds</div><div className="sub">Held safely until recipient claims</div></div>
            </div>
            <div className={'step ' + s(2)}>
              <div className="dot">{step > 2 && <Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/>}</div>
              <div className="body"><div className="t">Notifying recipient</div><div className="sub">Claim link sent to {phone}</div></div>
            </div>
          </div>
        </div>
        {complete && (
          <div style={{textAlign:'center',padding:'8px 0 16px'}}>
            <div style={{fontSize:14,color:'#86EFAC',fontWeight:600,marginBottom:4}}>Sent — awaiting claim</div>
            <div style={{fontSize:12,color:'var(--fg-3)',fontFamily:'var(--font-mono)'}}>48h claim window</div>
          </div>
        )}
      </div>
      <div style={{padding:'12px 20px 16px'}}>
        {complete
          ? <button className="sp-btn primary" onClick={onDone}>Done</button>
          : <button className="sp-btn secondary" disabled>Processing…</button>
        }
      </div>
    </>
  );
};

// =============== Remittance detail ===============
const DetailScreen = ({ txn, onBack }) => (
  <>
    <TopBar title="Remittance" onBack={onBack}/>
    <div className="sp-scroll">
      <div className="sp-detail-top">
        <div className="eyebrow">You sent</div>
        <div className="amt"><small>$</small>100.00</div>
        <div className="to">to {txn?.name || 'Raj Patel'} · ₹8,450.00</div>
        <div style={{marginTop:12}}>
          <span className="sp-badge esc"><span className="dot"></span>Escrowed · awaiting claim</span>
        </div>
      </div>
      <div className="sp-kv" style={{padding:'16px'}}>
        <div className="sp-timeline">
          <div className="step done"><div className="dot"><Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/></div><div className="body"><div className="t">Initiated</div><div className="sub">2m ago</div></div></div>
          <div className="step done"><div className="dot"><Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/></div><div className="body"><div className="t">Escrowed on Solana</div><div className="sub">5KWq…9xZ2 · 1m 52s ago</div></div></div>
          <div className="step done"><div className="dot"><Icon name="check" size={10} stroke="#0B1020" strokeWidth="3"/></div><div className="body"><div className="t">Claim SMS delivered</div><div className="sub">+91 98765 43210</div></div></div>
          <div className="step live"><div className="dot"></div><div className="body"><div className="t">Awaiting recipient claim</div><div className="sub">Expires in 47h 58m</div></div></div>
          <div className="step pending"><div className="dot"></div><div className="body"><div className="t">Delivery via UPI</div></div></div>
        </div>
      </div>
      <div className="sp-kv">
        <div className="r"><span className="k">Remittance ID</span><span className="v">8ce3…dc2a <Icon name="copy" size={12} stroke="var(--fg-3)"/></span></div>
        <div className="r"><span className="k">Escrow PDA</span><span className="v">7C2z…zWij</span></div>
        <div className="r"><span className="k">On-chain fee</span><span className="v" style={{color:'#86EFAC'}}>$0.002</span></div>
        <div className="r"><span className="k">FX rate</span><span className="v">84.50</span></div>
      </div>
    </div>
  </>
);

// =============== Add Funds ===============
const AddFundsScreen = ({ onBack, onDone }) => {
  const [amount, setAmount] = useState('50');
  const [step, setStep] = useState('amount'); // amount | paying | done
  const presets = ['25', '50', '100', '250'];

  const initiate = () => {
    setStep('paying');
    setTimeout(() => setStep('done'), 2200);
  };

  if (step === 'paying') return (
    <>
      <TopBar title="Add Funds" onBack={onBack}/>
      <div className="sp-scroll" style={{display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',flex:1,gap:20,paddingTop:40}}>
        <div style={{width:56,height:56,borderRadius:'50%',background:'var(--solana-gradient)',display:'grid',placeItems:'center',boxShadow:'var(--glow-solana-soft)',animation:'sp-pulse 1.4s ease-in-out infinite'}}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#0B1020" strokeWidth="2.2" strokeLinecap="round"><rect x="1" y="4" width="22" height="16" rx="2"/><path d="M1 10h22"/></svg>
        </div>
        <div style={{textAlign:'center'}}>
          <div style={{fontSize:15,fontWeight:600}}>Processing payment…</div>
          <div style={{fontSize:12,color:'var(--fg-3)',marginTop:6,fontFamily:'var(--font-mono)'}}>Stripe · ${parseFloat(amount||0).toFixed(2)} USD</div>
        </div>
      </div>
    </>
  );

  if (step === 'done') return (
    <>
      <TopBar title="Add Funds" onBack={onDone}/>
      <div className="sp-scroll" style={{display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',flex:1,gap:16,paddingTop:40}}>
        <div style={{width:72,height:72,borderRadius:'50%',background:'var(--success-soft)',border:'1px solid var(--success-border)',display:'grid',placeItems:'center',margin:'0 auto 8px'}}>
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5l9-10"/></svg>
        </div>
        <div style={{textAlign:'center'}}>
          <div style={{fontSize:22,fontWeight:700,fontFamily:'var(--font-mono)'}}>${parseFloat(amount||0).toFixed(2)} USDC</div>
          <div style={{fontSize:14,color:'var(--fg-2)',marginTop:6}}>Added to your wallet</div>
        </div>
        <button className="sp-btn primary" style={{marginTop:16,width:'80%'}} onClick={onDone}>Done</button>
      </div>
    </>
  );

  const val = parseFloat(amount || 0);
  const valid = val >= 1 && val <= 10000;

  return (
    <>
      <TopBar title="Add Funds" onBack={onBack}/>
      <div className="sp-scroll">
        <div className="sp-amount-input" style={{paddingBottom:16}}>
          <div className="eyebrow">Amount (USD)</div>
          <div style={{display:'flex',alignItems:'baseline',justifyContent:'center',gap:4}}>
            <small style={{color:'var(--fg-3)',fontFamily:'var(--font-mono)',fontSize:40}}>$</small>
            <input className="field" value={amount} onChange={e=>setAmount(e.target.value.replace(/[^\d.]/g,''))} style={{maxWidth:(amount.length+1)+'ch'}}/>
          </div>
          <div style={{fontSize:12,color:val>10000?'var(--danger)':'var(--fg-3)',marginTop:8,fontFamily:'var(--font-mono)'}}>Min $1.00 · Max $10,000.00</div>
        </div>

        <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:8,marginBottom:20}}>
          {presets.map(p=>(
            <button key={p} onClick={()=>setAmount(p)} style={{padding:'10px 4px',borderRadius:10,border:'1px solid',borderColor:amount===p?'var(--accent)':'var(--border-2)',background:amount===p?'var(--accent-soft)':'var(--surface-2)',color:amount===p?'#D6B4FF':'var(--fg-2)',fontFamily:'var(--font-mono)',fontSize:14,cursor:'pointer',fontWeight:600}}>
              ${p}
            </button>
          ))}
        </div>

        <div className="sp-kv" style={{marginBottom:20}}>
          <div className="r"><span className="k">Payment method</span><span className="v">Credit / Debit card</span></div>
          <div className="r"><span className="k">Powered by</span><span className="v">Stripe</span></div>
        </div>

        <button className="sp-btn primary" onClick={initiate} disabled={!valid}>
          Pay ${valid?val.toFixed(2):'—'} with Stripe
        </button>
        <div style={{fontSize:11,color:'var(--fg-3)',textAlign:'center',marginTop:12,fontFamily:'var(--font-mono)'}}>Secured by Stripe · Funds appear instantly</div>
      </div>
    </>
  );
};


const ActivityScreen = ({ onOpen }) => {
  const all = [
    { name: 'Raj Patel', sub: '+91 98765 43210 · 2m ago', amt: '100.00 USDC', status: 'esc', statusLbl: 'Escrowed'},
    { name: 'Meera Iyer', sub: '+91 99887 66554 · yesterday', amt: '250.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
    { name: 'Vikram Shah', sub: '+91 99001 23456 · 3 days ago', amt: '75.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
    { name: 'Ananya Rao', sub: '+91 90000 12345 · 1 week ago', amt: '200.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
    { name: 'Suresh Kumar', sub: '+91 91234 56789 · 2 weeks ago', amt: '50.00 USDC', status: 'delivered', statusLbl: 'Delivered'},
  ];
  return (
    <>
      <TopBar title="Activity"/>
      <div className="sp-scroll">
        <div className="sp-section-h" style={{marginTop:8}}><h3>All transfers</h3></div>
        {all.map((t,i) => (
          <div key={i} className="sp-row" onClick={() => onOpen(t)}>
            <div className="av">{t.name.split(' ').map(n=>n[0]).join('')}</div>
            <div className="mid">
              <div className="name">{t.name}</div>
              <div className="sub">{t.sub}</div>
            </div>
            <div className="right">
              <div className="amt">-{t.amt}</div>
              <div className="s" style={{color: t.status==='esc' ? '#D6B4FF' : '#86EFAC'}}>{t.statusLbl}</div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
};

// =============== Me ===============
const MeScreen = ({ onSignOut }) => (
  <>
    <TopBar title="Account"/>
    <div className="sp-scroll">
      <div style={{textAlign:'center', padding:'24px 0 28px'}}>
        <div style={{width:72,height:72,borderRadius:'50%',background:'var(--solana-gradient)',display:'grid',placeItems:'center',margin:'0 auto 14px',fontSize:28,fontWeight:700,color:'#0B1020'}}>R</div>
        <div style={{fontSize:18,fontWeight:600}}>Raj Sharma</div>
        <div style={{fontSize:13,color:'var(--fg-3)',fontFamily:'var(--font-mono)',marginTop:4}}>raj@gmail.com</div>
      </div>
      <div className="sp-kv">
        <div className="r"><span className="k">USD Balance</span><span className="v" style={{color:'var(--fg-1)'}}>$248.50</span></div>
        <div className="r"><span className="k">Member since</span><span className="v">Jan 2025</span></div>
      </div>
      <div className="sp-kv">
        <div className="r"><span className="k">Wallet</span><span className="v" style={{fontSize:11}}>CrsMd…DAd18</span></div>
        <div className="r"><span className="k">Network</span><span className="v">Solana Mainnet</span></div>
        <div className="r"><span className="k">KYC status</span><span className="v" style={{color:'#86EFAC'}}>Verified</span></div>
      </div>
      <div className="sp-kv">
        <div className="r" style={{cursor:'pointer'}}><span className="k">Notifications</span><span className="v">On</span></div>
        <div className="r" style={{cursor:'pointer'}}><span className="k">Support</span><span className="v">→</span></div>
        <div className="r" style={{cursor:'pointer'}} onClick={onSignOut}><span className="k" style={{color:'#EF4444'}}>Sign out</span><span className="v"></span></div>
      </div>
    </div>
  </>
);

// Expose
Object.assign(window, { Icon, GradDefs, AuthScreen, TopBar, TabBar, HomeScreen, SendAmount, SendRecipient, SendReview, SendingScreen, DetailScreen, ActivityScreen, MeScreen, AddFundsScreen });

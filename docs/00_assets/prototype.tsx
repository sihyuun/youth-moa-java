// @ts-nocheck
// Youth-Moa Prototype — 최신 동기화본 (2026-07-03)
// 상단 뒤로가기 버튼 아이콘 전용화(admin 패턴: 테두리 정사각 + chevron)
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { createRoot } from 'react-dom/client';

// ── Design Tokens ──
const T = {
	primary: '#3F30E9', primaryDark: '#3428CF',
	primaryLight: 'oklch(0.93 0.0425 280)', primaryBg: 'oklch(0.96 0.0255 280)',
	secondary: '#F97316', secondaryLight: '#FFEDD5', success: '#10B981', successLight: '#D1FAE5',
	warning: '#F59E0B', warningLight: '#FEF3C7', error: '#EF4444',
	bg: 'oklch(0.985 0.004 280)', surface: '#FFFFFF',
	text: 'oklch(0.22 0.051 280)', textSec: 'oklch(0.55 0.034 280)', textTri: 'oklch(0.7 0.02 280)',
	border: 'oklch(0.9 0.014 280)', borderLight: 'oklch(0.95 0.007 280)',
	shadow: '0 1px 3px rgba(63,48,233,0.06)', shadowMd: '0 4px 12px rgba(63,48,233,0.07)',
	shadowLg: '0 10px 24px rgba(63,48,233,0.09)',
	radius: 12, tagR: 20, headerH: 68,
};

const IMGS = {
	banner: 'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=1440&h=560&fit=crop',
	// 히어로 배경 로테이션 — 8초마다 크로스페이드 전환 (Method A scrim 오버레이)
	banners: [
		'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=1440&h=560&fit=crop', // A 협업 테이블
		'https://images.unsplash.com/photo-1543269865-cbf427effbad?w=1440&h=560&fit=crop',     // C 학습 그룹
		'https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=1440&h=560&fit=crop',  // E 세미나·강연장
		'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=1440&h=560&fit=crop',  // F 야외 청년 모임
		'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=1440&h=560&fit=crop',  // G 커뮤니티 모임
		'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1440&h=560&fit=crop',  // H 행사·관객
	],
	pg: ['https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1531482615713-2afd69097998?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=280&fit=crop'],
	space: ['https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop','https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=460&h=340&fit=crop','https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=460&h=340&fit=crop'],
};

const PROGRAMS = [
	{ id:1, title:'취업역량 강화 워크숍', center:'내일스퀘어', region:'부천시', status:'진행중', date:'2024-08-01~08-31', cap:30, applied:27, dday:3, cat:'취업' },
	{ id:2, title:'청년 창업 아카데미', center:'상상대로', region:'수원시', status:'진행중', date:'2024-08-01~08-31', cap:25, applied:12, dday:12, cat:'창업' },
	{ id:3, title:'마음건강 힐링 캠프', center:'범계역 청년출구', region:'안양시', status:'진행중', date:'2024-07-01~07-31', cap:20, applied:18, dday:6, cat:'힐링' },
	{ id:4, title:'디지털 마케팅 실전반', center:'원미청정구역', region:'부천시', status:'마감', date:'2024-07-01~07-31', cap:15, applied:15, dday:0, cat:'교육' },
	{ id:5, title:'AI 활용 실무 교육', center:'비행지구', region:'고양시', status:'진행예정', date:'2024-09-01~09-30', apply:'2024-08-20~08-31', cap:30, applied:0, openDday:14, cat:'교육' },
	{ id:6, title:'소셜벤처 인큐베이팅', center:'오름', region:'용인시', status:'진행예정', date:'2024-09-05~09-30', apply:'2024-08-25~09-02', cap:20, applied:0, openDday:21, cat:'창업' },
	{ id:7, title:'청년 목공 클래스 (비활성 데모)', center:'메이커스페이스', region:'성남시', status:'중단', date:'2024-08-10~08-30', cap:16, applied:9, dday:5, cat:'교육', demo:true },
];

const NOTICES = [
	{ id:1, cat:'행사', title:'제1회 청년의 날 축제 안내', date:'2024.07.30', views:756, pin:true },
	{ id:2, cat:'공지', title:'7월 청년센터 프로그램 일정 안내', date:'2024.06.15', views:7178 },
	{ id:3, cat:'운영', title:'7월 휴관 일정 안내', date:'2024.06.15', views:7756 },
	{ id:4, cat:'기타', title:'[경기도] 2024 경기 사회적 경제 박람회', date:'2024.05.15', views:157 },
];

// ── Icon ──
function Icon({ n, size=20, color='currentColor', style:xs, onClick }) {
	const paths = {
		bell: <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
		search: <><circle cx="11" cy="11" r="8" stroke={color} strokeWidth="1.5" fill="none"/><path d="M21 21l-4.35-4.35" stroke={color} strokeWidth="1.5" strokeLinecap="round"/></>,
		star: <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>,
		starFill: <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill={color} stroke={color} strokeWidth="1"/>,
		check: <><circle cx="12" cy="12" r="10" stroke={color} strokeWidth="1.5" fill="none"/><path d="M9 12l2 2 4-4" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
		chevR: <path d="M9 18l6-6-6-6" stroke={color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
		chevD: <path d="M6 9l6 6 6-6" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
		pin: <><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="12" cy="10" r="3" stroke={color} strokeWidth="1.5" fill="none"/></>,
		share: <><circle cx="18" cy="5" r="3" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="6" cy="12" r="3" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="18" cy="19" r="3" stroke={color} strokeWidth="1.5" fill="none"/><path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 3.98" stroke={color} strokeWidth="1.5" fill="none"/></>,
		close: <path d="M18 6L6 18M6 6l12 12" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
		mail: <><rect x="3" y="5" width="18" height="14" rx="2" stroke={color} strokeWidth="1.5" fill="none"/><path d="M3 7l9 6 9-6" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
		login: <><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
		user: <><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="12" cy="7" r="4" stroke={color} strokeWidth="1.5" fill="none"/></>,
		download: <><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
		upload: <><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
		filter: <path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>,
		arrowL: <path d="M19 12H5M12 5l-7 7 7 7" stroke={color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
		calendar: <><rect x="3" y="4" width="18" height="18" rx="2" stroke={color} strokeWidth="1.5" fill="none"/><path d="M16 2v4M8 2v4M3 10h18" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round"/></>,
		grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" stroke={color} strokeWidth="1.6" fill="none"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke={color} strokeWidth="1.6" fill="none"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke={color} strokeWidth="1.6" fill="none"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke={color} strokeWidth="1.6" fill="none"/></>,
	};
	return <svg onClick={onClick} viewBox="0 0 24 24" style={{ width:size, height:size, flexShrink:0, ...xs }}>{paths[n]||null}</svg>;
}

// ── Shared Button ──
function Btn({ children, variant='primary', size='m', fullWidth, icon, onClick, disabled, style:xs }) {
	const sizes = { s:{h:34,px:14,fs:13}, m:{h:42,px:20,fs:14}, l:{h:50,px:28,fs:16} };
	const s = sizes[size];
	const vars = {
		primary: { bg:T.primary, fg:'#fff', border:'none' },
		secondary: { bg:T.surface, fg:T.text, border:`1px solid ${T.text}` },
		outline: { bg:'transparent', fg:T.primary, border:`1px solid ${T.primary}` },
		ghost: { bg:'transparent', fg:T.textSec, border:`1px solid ${T.border}` },
		danger: { bg:T.error, fg:'#fff', border:'none' },
		dangerOutline: { bg:T.surface, fg:T.error, border:`1px solid ${T.error}` },
	};
	const v = vars[variant]||vars.primary;
	return (
		<div className="btn-hover" onClick={disabled?undefined:onClick} style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', gap:6, height:s.h, padding:`0 ${s.px}px`, borderRadius:8, background:disabled?T.border:v.bg, color:disabled?T.textTri:v.fg, border:v.border, fontSize:s.fs, fontWeight:600, cursor:disabled?'not-allowed':'pointer', width:fullWidth?'100%':'auto', boxSizing:'border-box', userSelect:'none', opacity:disabled?0.7:1, ...xs }}>
			{icon && <Icon n={icon} size={s.fs+2} color={disabled?T.textTri:v.fg}/>}
			{children}
		</div>
	);
}

// ── Badge ──
function Badge({ text, variant='primary' }) {
	// 파스텔 통일안: 연한 배경 + 진한 텍스트 (전체 페이지 공통)
	const v = { primary:{bg:T.primaryLight,fg:T.primary}, muted:{bg:T.borderLight,fg:T.textTri}, success:{bg:T.successLight,fg:T.success}, warning:{bg:T.warningLight,fg:T.warning}, secondary:{bg:T.secondaryLight,fg:T.secondary} };
	const c = v[variant]||v.primary;
	return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 10px', borderRadius:T.tagR, background:c.bg, color:c.fg, fontSize:12, fontWeight:600, lineHeight:'18px' }}>{text}</span>;
}

// ── Toast ──
// ── Skeleton (로딩 플레이스홀더) ──
function ProgramCardSkeleton() {
	return (
		<div style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, border:`1px solid ${T.borderLight}` }}>
			<div className="skeleton" style={{ width:'100%', height:160, borderRadius:0 }}/>
			<div style={{ padding:'12px 14px 14px' }}>
				<div className="skeleton" style={{ width:'80%', height:15, marginBottom:8 }}/>
				<div className="skeleton" style={{ width:'50%', height:12, marginBottom:12 }}/>
				<div className="skeleton" style={{ width:'100%', height:8, marginBottom:12 }}/>
				<div className="skeleton" style={{ width:'100%', height:34, borderRadius:20 }}/>
			</div>
		</div>
	);
}

function Toast({ msg, onDone }) {
	useEffect(() => { const t = setTimeout(onDone, 2800); return ()=>clearTimeout(t); }, []);
	return (
		<div style={{ position:'fixed', top:90, left:'50%', transform:'translateX(-50%)', zIndex:1000, display:'flex', alignItems:'center', gap:10, padding:'14px 24px', background:T.surface, borderRadius:12, boxShadow:'0 8px 32px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, animation:'slideDown 300ms ease forwards' }}>
			<div style={{ width:22, height:22, borderRadius:'50%', background:'#22C55E', display:'flex', alignItems:'center', justifyContent:'center' }}>
				<svg viewBox="0 0 24 24" style={{width:14,height:14}}><path d="M9 12l2 2 4-4" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
			</div>
			<span style={{ fontSize:15, fontWeight:500, color:T.text }}>{msg}</span>
		</div>
	);
}

// ── Modal backdrop ──
function Modal({ children, onClose }) {
	useEffect(()=>{ const h=e=>e.key==='Escape'&&onClose&&onClose(); window.addEventListener('keydown',h); return ()=>window.removeEventListener('keydown',h); },[onClose]);
	return (
		<div className="overlay-enter" onClick={onClose} style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.45)', zIndex:500, display:'flex', alignItems:'center', justifyContent:'center' }}>
			<div onClick={e=>e.stopPropagation()}>{children}</div>
		</div>
	);
}

// ── ModalCard: 공통 모달 카드 셸 (제목 + 닫기 + 본문) ──
function ModalCard({ title, onClose, width=460, children, footer }) {
	return (
		<Modal onClose={onClose}>
			<div className="dropdown-enter" style={{ width, maxWidth:'90vw', background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.18)', overflow:'hidden' }}>
				{title && (
					<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'18px 22px', borderBottom:`1px solid ${T.borderLight}` }}>
						<span style={{ fontSize:16, fontWeight:700, color:T.text }}>{title}</span>
						<div className="btn-hover" onClick={onClose} style={{ cursor:'pointer', display:'flex' }}><Icon n="close" size={20} color={T.textTri}/></div>
					</div>
				)}
				<div style={{ padding:'20px 22px' }}>{children}</div>
				{footer && <div style={{ padding:'0 22px 20px' }}>{footer}</div>}
			</div>
		</Modal>
	);
}

// ── ConfirmDialog: 공통 확인/알림 다이얼로그 ──
// variant: 'default' | 'danger' | 'success'  /  alert={true} → 단일 확인 버튼
function ConfirmDialog({ icon, variant='default', title, message, confirmText='확인', cancelText='취소', alert=false, disabled=false, onConfirm, onClose, children }) {
	const tone = variant==='danger' ? { c:T.error, bg:'#FEF2F2' } : variant==='success' ? { c:T.success, bg:T.successLight } : { c:T.primary, bg:T.primaryBg };
	return (
		<Modal onClose={onClose}>
			<div className="dropdown-enter" style={{ width:440, maxWidth:'90vw', padding:'32px 28px 24px', background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.18)', textAlign:'center' }}>
				{icon && (
					<div style={{ width:56, height:56, borderRadius:'50%', background:tone.bg, display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
						<Icon n={icon} size={28} color={tone.c}/>
					</div>
				)}
				{title && <h3 style={{ fontSize:19, fontWeight:700, color:T.text, marginBottom:8, wordBreak:'keep-all' }}>{title}</h3>}
				{message && <p style={{ fontSize:14, color:T.textSec, lineHeight:1.6, marginBottom:children?16:4, wordBreak:'keep-all' }}>{message}</p>}
				{children && <div style={{ textAlign:'left', marginBottom:4 }}>{children}</div>}
				<div style={{ display:'flex', gap:10, marginTop:20 }}>
					{!alert && <Btn size="m" variant="ghost" fullWidth onClick={onClose}>{cancelText}</Btn>}
					<Btn size="m" variant={variant==='danger'?'danger':'primary'} fullWidth disabled={disabled} onClick={onConfirm}>{confirmText}</Btn>
				</div>
			</div>
		</Modal>
	);
}

// ── Capacity / D-day helpers ──
function capInfo(pg){
	const upcoming = pg.status==='진행예정';
	const inactive = pg.inactive || pg.status==='중단';
	const applied = pg.applied ?? Math.round(pg.cap*0.6);
	const ratio = Math.min(applied/pg.cap, 1);
	const pct = Math.round(ratio*100);
	const full = pg.status==='마감' || applied>=pg.cap;
	let color=T.primary, label='모집중';
	if(inactive){ color=T.textTri; label='모집 중단'; }
	else if(upcoming){ color=T.secondary; label='진행예정'; }
	else if(full){ color=T.textTri; label='마감'; }
	else if(ratio>=0.9){ color=T.error; label='마감임박'; }
	else if(ratio>=0.7){ color=T.warning; label='서두르세요'; }
	return { applied, ratio, pct, full, upcoming, inactive, color, label };
}
function CapacityBar({ pg, showLabel=true }){
	const c = capInfo(pg);
	if(c.upcoming) return (
		<div style={{ width:'100%' }}>
			{showLabel && <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:5 }}>
				<span style={{ fontSize:12, fontWeight:600, color:c.color }}>신청 오픈 예정</span>
				<span style={{ fontSize:11, color:T.textTri }}>{pg.apply? pg.apply.split('~')[0].slice(5).replace('-','/')+' 오픈':''}</span>
			</div>}
			<div style={{ width:'100%', height:6, borderRadius:3, background:T.borderLight, overflow:'hidden' }}>
				<div style={{ width:'0%', height:'100%', borderRadius:3, background:c.color }}/>
			</div>
		</div>
	);
	return (
		<div style={{ width:'100%' }}>
			{showLabel && <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:5 }}>
				<span style={{ fontSize:12, fontWeight:600, color:c.color }}>{c.full?'모집 마감':`정원 ${c.applied}/${pg.cap}명`}</span>
				<span style={{ fontSize:11, color:T.textTri }}>{c.pct}%</span>
			</div>}
			<div style={{ width:'100%', height:6, borderRadius:3, background:T.borderLight, overflow:'hidden' }}>
				<div style={{ width:`${c.pct}%`, height:'100%', borderRadius:3, background:c.color }}/>
			</div>
		</div>
	);
}
function DdayChip({ pg }){
	const c = capInfo(pg);
	if(c.inactive) return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 9px', borderRadius:T.tagR, background:'rgba(0,0,0,0.55)', color:'#fff', fontSize:11, fontWeight:700 }}>운영중단</span>;
	if(c.upcoming) return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 9px', borderRadius:T.tagR, background:T.secondary, color:'#fff', fontSize:11, fontWeight:700 }}>오픈 D-{pg.openDday??'-'}</span>;
	if(c.full) return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 9px', borderRadius:T.tagR, background:'rgba(0,0,0,0.55)', color:'#fff', fontSize:11, fontWeight:700 }}>마감</span>;
	const urgent = (pg.dday??99)<=3;
	return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 9px', borderRadius:T.tagR, background:urgent?T.error:'rgba(0,0,0,0.55)', color:'#fff', fontSize:11, fontWeight:700 }}>{pg.dday===0?'D-DAY':`D-${pg.dday??'-'}`}</span>;
}
function Toggle({ on, onClick }){
	return <div onClick={onClick} style={{ width:44, height:26, borderRadius:13, background:on?T.primary:T.border, position:'relative', flexShrink:0, cursor:'pointer', transition:'background 200ms' }}>
		<div style={{ width:20, height:20, borderRadius:'50%', background:'#fff', position:'absolute', top:3, left:on?21:3, transition:'left 200ms', boxShadow:'0 1px 3px rgba(0,0,0,0.2)' }}/>
	</div>;
}
// ── Waitlist (빈자리 알림) Modal ──
function WaitlistModal({ pg, onClose, addToast }){
	const [ch, setCh] = useState({ kakao:true, email:true });
	const [done, setDone] = useState(false);
	const submit = () => { setDone(true); addToast && addToast('빈자리 알림을 신청했어요.'); setTimeout(onClose, 1100); };
	return (
		<Modal onClose={onClose}>
			<div style={{ width:440, background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.18)', overflow:'hidden' }}>
				<div style={{ padding:'24px 28px 0', display:'flex', justifyContent:'flex-end' }}><div className="btn-hover" onClick={onClose} style={{cursor:'pointer'}}><Icon n="close" size={22} color={T.textTri}/></div></div>
				<div style={{ padding:'0 32px 30px', textAlign:'center' }}>
					<div style={{ width:56, height:56, borderRadius:'50%', background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}><Icon n="bell" size={26} color={T.primary}/></div>
					<h3 style={{ fontSize:20, fontWeight:700, color:T.text, margin:'0 0 8px' }}>빈자리 알림 받기</h3>
					<p style={{ fontSize:14, color:T.textSec, lineHeight:1.6, margin:'0 0 20px' }}>빈자리가 생기면<br/>아래의 수단으로 <strong style={{color:T.text}}>알림을 보내드려요.</strong></p>
					<div style={{ display:'flex', gap:12, padding:14, borderRadius:T.radius, background:T.bg, border:`1px solid ${T.borderLight}`, textAlign:'left', marginBottom:20 }}>
						<div style={{ width:48, height:48, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
						<div style={{ flex:1, minWidth:0 }}>
							<div style={{ fontSize:14, fontWeight:600, color:T.text, marginBottom:3, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
							<div style={{ fontSize:12, color:T.textSec }}>{pg.center} · 정원 {pg.cap}/{pg.cap}명 (마감)</div>
						</div>
					</div>
					<div style={{ textAlign:'left', marginBottom:20 }}>
						<span style={{ fontSize:13, fontWeight:600, color:T.textSec, display:'block', marginBottom:10 }}>알림 받을 방법 <span style={{ fontWeight:400, color:T.textTri }}>(여러 개 선택 가능)</span></span>
						<div style={{ display:'flex', flexDirection:'column', gap:8 }}>
							{[{k:'kakao',label:'카카오 알림톡',sub:'010-1234-5678'},{k:'email',label:'이메일',sub:'hyuuun0321@naver.com'}].map(opt=>{
								const on = ch[opt.k];
								return <label key={opt.k} onClick={()=>setCh(s=>({...s,[opt.k]:!s[opt.k]}))} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 14px', borderRadius:10, border:`1px solid ${on?T.primary:T.border}`, background:on?T.primaryBg:T.surface, cursor:'pointer' }}>
									<div style={{ flex:1 }}>
										<div style={{ fontSize:13.5, fontWeight:600, color:T.text }}>{opt.label}</div>
										<div style={{ fontSize:11.5, color:T.textTri }}>{opt.sub}</div>
									</div>
									<div style={{ width:20, height:20, borderRadius:6, border:`1.5px solid ${on?T.primary:T.border}`, background:on?T.primary:T.surface, display:'flex', alignItems:'center', justifyContent:'center' }}>
										{on && <svg viewBox="0 0 24 24" style={{width:13,height:13}}><path d="M5 12l4 4 10-10" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
									</div>
								</label>;
							})}
						</div>
					</div>
					<div style={{ display:'flex', gap:10 }}>
						<Btn size="l" variant="ghost" onClick={onClose} style={{flex:'0 0 110px'}}>취소</Btn>
						<Btn size="l" variant="primary" icon="bell" fullWidth onClick={submit} style={{flex:1}}>{done?'신청 완료':'알림 받기'}</Btn>
					</div>
				</div>
			</div>
		</Modal>
	);
}

// ── Notification Panel ──
function NotifPanel({ onClose }) {
	const [items, setItems] = useState([
		{ id:1, title:'프로그램 신청 승인', body:'[취업역량 강화 워크숍] 신청이 승인되었습니다.', time:'1시간 전', unread:true, icon:'check', tone:T.success },
		{ id:2, title:'마감 임박', body:'[청년 창업 아카데미] 마감이 임박했어요. (D-1)', time:'3시간 전', unread:true, icon:'calendar', tone:T.warning },
		{ id:3, title:'공지사항', body:'새 공지사항 — 7월 휴관 일정 안내', time:'1일 전', unread:false, icon:'bell', tone:T.primary },
		{ id:4, title:'신청 취소 처리', body:'[마음건강 힐링 캠프] 취소가 처리되었습니다.', time:'2일 전', unread:false, icon:'close', tone:T.textTri },
	]);
	const unreadCount = items.filter(i=>i.unread).length;
	const markAllRead = () => setItems(prev=>prev.map(i=>({...i,unread:false})));
	const clearAll = () => setItems([]);
	const remove = (id) => setItems(prev=>prev.filter(i=>i.id!==id));
	return (
		<div className="dropdown-enter" style={{ width:380, background:T.surface, borderRadius:14, boxShadow:'0 12px 40px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, overflow:'hidden' }}>
			<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'16px 18px', borderBottom:`1px solid ${T.borderLight}` }}>
				<div style={{ display:'flex', alignItems:'center', gap:6 }}>
					<span style={{ fontSize:16, fontWeight:700, color:T.text }}>알림</span>
					{unreadCount>0 && <span style={{ minWidth:18, height:18, padding:'0 5px', borderRadius:9, background:T.error, color:'#fff', fontSize:11, fontWeight:700, display:'flex', alignItems:'center', justifyContent:'center' }}>{unreadCount}</span>}
				</div>
				<div style={{ display:'flex', alignItems:'center', gap:8 }}>
					<span onClick={markAllRead} className="btn-hover" style={{ fontSize:12, color:T.textSec, cursor:'pointer' }}>모두 읽음</span>
					<span style={{ fontSize:12, color:T.border }}>|</span>
					<span onClick={clearAll} className="btn-hover" style={{ fontSize:12, color:T.textSec, cursor:'pointer' }}>모두 지우기</span>
				</div>
			</div>
			{items.length===0 ? (
				<div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'40px 0', gap:12 }}>
					<Icon n="bell" size={40} color={T.border}/>
					<span style={{ fontSize:14, color:T.textSec }}>아직 도착한 알림이 없어요</span>
				</div>
			) : items.map(item=>(
				<div key={item.id} style={{ display:'flex', alignItems:'flex-start', gap:12, padding:'14px 16px', borderBottom:`1px solid ${T.borderLight}`, background:item.unread?T.primaryBg:T.surface }}>
					<div style={{ width:36, height:36, borderRadius:'50%', background:item.unread?(item.tone||T.primary):'#E5E7EB', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
						<Icon n={item.icon||'bell'} size={16} color={item.unread?'#fff':'#9CA3AF'}/>
					</div>
					<div style={{ flex:1, minWidth:0 }}>
						<div style={{ fontSize:14, color:item.unread?T.text:T.textSec, fontWeight:item.unread?600:400, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{item.title}</div>
						<div style={{ fontSize:13, color:T.textSec, marginTop:2, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{item.body}</div>
						<div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>{item.time}</div>
					</div>
					<div onClick={()=>remove(item.id)} className="btn-hover" style={{ cursor:'pointer', flexShrink:0, marginTop:2 }}>
						<Icon n="close" size={16} color={T.textTri}/>
					</div>
				</div>
			))}
			<div style={{ padding:'12px 18px', textAlign:'center', cursor:'pointer', fontSize:13, fontWeight:600, color:T.primary }} className="btn-hover">알림 전체보기</div>
		</div>
	);
}

// ── Avatar (이미지 업로드 지원, 디폴트는 이름 이니셜) ──
const PROFILE_IMG = null; // 업로드 시 이미지 URL, 없으면 이름 이니셜
function Avatar({ size=38, name='박', transparent=false }) {
	const fs = Math.round(size*0.42);
	return (
		<div style={{ width:size, height:size, borderRadius:'50%', flexShrink:0, overflow:'hidden', background:transparent?'rgba(255,255,255,0.25)':T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
			{PROFILE_IMG
				? <img src={PROFILE_IMG} alt="프로필" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
				: <span style={{ fontWeight:700, fontSize:fs, color:transparent?'#fff':T.primary }}>{name}</span>}
		</div>
	);
}

// ── User Menu ──
function UserMenuPanel({ onClose, onLogout, onMenu }) {
	return (
		<div className="dropdown-enter" style={{ width:220, background:T.surface, borderRadius:14, boxShadow:'0 12px 40px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, overflow:'hidden' }}>
			<div onClick={()=>{onClose();onMenu('history');}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:10, padding:'16px 18px', borderBottom:`1px solid ${T.borderLight}`, cursor:'pointer' }}>
				<Avatar size={38}/>
				<div style={{ flex:1, minWidth:0 }}>
					<div style={{ fontSize:15, fontWeight:600, color:T.text }}>박시현님</div>
					<div style={{ fontSize:12, color:T.textTri, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>hyuuun0321@naver.com</div>
				</div>
				<Icon n="chevR" size={15} color={T.textTri}/>
			</div>
			<div style={{ borderTop:'none' }}>
				<div onClick={()=>{onClose();onLogout();}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:10, padding:'11px 18px', cursor:'pointer' }}>
					<Icon n="login" size={17} color={T.textTri} style={{transform:'scaleX(-1)'}}/>
					<span style={{ fontSize:14, color:T.textSec }}>로그아웃</span>
				</div>
			</div>
		</div>
	);
}

// ── Header ──
function Header({ screen, go, isLoggedIn, onLoginClick, onLogout, onMyPage, scrolled }) {
	const [showNotif, setShowNotif] = useState(false);
	const [showUser, setShowUser] = useState(false);
	const isHome = screen==='home';
	const transparent = isHome && !scrolled;
	const fg = transparent ? '#fff' : T.text;
	const fgSec = transparent ? 'rgba(255,255,255,0.85)' : T.textSec;
	const nav = [
		{ label:'프로그램', screen:'programs' },
		{ label:'청년센터', screen:'centers' },
		{ label:'공지사항', screen:'notices' },
	];
	return (
		<div style={{ width:'100%', height:T.headerH, background:transparent?'transparent':T.surface, borderBottom:transparent?'none':`1px solid ${T.border}`, display:'flex', alignItems:'center', padding:'0 80px', boxSizing:'border-box', position:'relative', zIndex:100, transition:'background 300ms ease, border-color 300ms ease, box-shadow 300ms ease', boxShadow:transparent?'none':'0 1px 8px rgba(0,0,0,0.06)' }}>
			{/* Logo — symbol + text */}
			<div style={{ flex:1 }}>
				<div onClick={()=>go('home')} style={{ display:'inline-flex', alignItems:'center', gap:8, cursor:'pointer' }}>
					<img src="assets/logo_symbol.png" alt="" style={{ height:34, width:'auto', filter:transparent?'brightness(0) invert(1)':'none', transition:'filter 300ms' }}/>
					<span style={{ fontWeight:800, fontSize:18, letterSpacing:-0.3, color:transparent?'#fff':T.primary, transition:'color 300ms' }}>청년모아</span>
				</div>
			</div>
			{/* Nav */}
			<div style={{ display:'flex', gap:36 }}>
				{nav.map(n => (
					<span key={n.label} className="nav-link" onClick={()=>go(n.screen)} style={{ fontSize:15, fontWeight:screen===n.screen?600:400, color:transparent?(screen===n.screen?'#fff':'rgba(255,255,255,0.88)'):(screen===n.screen?T.primary:T.text), paddingBottom:4, borderBottom:screen===n.screen?`2px solid ${transparent?'#fff':T.primary}`:'2px solid transparent' }}>{n.label}</span>
				))}
			</div>
			{/* Right */}
			<div style={{ flex:1, display:'flex', gap:16, alignItems:'center', justifyContent:'flex-end', position:'relative' }}>
				<Icon n="search" size={20} color={fgSec} style={{cursor:'pointer'}} onClick={()=>go('search')}/>
				{isLoggedIn && (
					<div style={{ position:'relative' }}>
						<div style={{ position:'relative', cursor:'pointer' }} onClick={()=>{setShowNotif(v=>!v); setShowUser(false);}}>
							<Icon n="bell" size={20} color={fgSec}/>
							<span style={{ position:'absolute', top:-2, right:-2, width:7, height:7, borderRadius:'50%', background:T.error, border:`1.5px solid ${transparent?'transparent':T.surface}` }}/>
						</div>
						{showNotif && (
							<div style={{ position:'absolute', top:34, right:-10, zIndex:200 }}>
								<NotifPanel onClose={()=>setShowNotif(false)}/>
							</div>
						)}
					</div>
				)}
				{isLoggedIn ? (
					<div style={{ position:'relative' }}>
						<div className="btn-hover" onClick={()=>{setShowUser(v=>!v); setShowNotif(false);}} style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer' }}>
							<Avatar size={30} transparent={transparent}/>
							<span style={{ fontSize:14, fontWeight:500, color:transparent?'#fff':T.text }}>박시현님</span>
							<Icon n="chevD" size={14} color={transparent?'rgba(255,255,255,0.8)':T.textSec}/>
						</div>
						{showUser && (
							<div style={{ position:'absolute', top:40, right:0, zIndex:200 }}>
								<UserMenuPanel onClose={()=>setShowUser(false)} onLogout={onLogout} onMenu={(tab)=>go('mypage',{tab})}/>
							</div>
						)}
					</div>
				) : (
					<Icon n="login" size={21} color={transparent?'#fff':T.primary} style={{cursor:'pointer'}} onClick={onLoginClick}/>
				)}
			</div>
		</div>
	);
}

// ── Footer ──
function Footer() {
	const links = ['개인정보처리방침','이용약관','이메일주소무단수집거부'];
	const socials = [
		{ l:'Instagram', src:'assets/sns_instagram.png' },
		{ l:'YouTube', src:'assets/sns_youtube.png' },
		{ l:'KakaoTalk', src:'assets/sns_kakaotalk.png' },
		{ l:'Facebook', src:'assets/sns_facebook.png' },
	];
	return (
		<div style={{ background:'#F5F5F5', borderTop:`1px solid ${T.border}`, padding:'20px 80px 16px' }}>
			<div style={{ display:'flex', alignItems:'center', gap:40 }}>
				<div style={{ display:'inline-flex', alignItems:'center', gap:7, flexShrink:0 }}>
					<img src="assets/logo_symbol.png" alt="" style={{ height:28, width:'auto' }}/>
					<span style={{ fontWeight:800, fontSize:16, letterSpacing:-0.3, color:T.primary }}>청년모아</span>
				</div>
				<div style={{ flex:1 }}>
					<div style={{ display:'flex', gap:16, marginBottom:6, alignItems:'center' }}>
						{links.map(l=><span key={l} style={{ fontSize:11, fontWeight:500, color:'#444', cursor:'pointer' }}>{l}</span>)}
						<span style={{ width:1, height:10, background:'#ccc' }}/>
						<span onClick={()=>window.__ymGo && window.__ymGo('forbidden')} style={{ fontSize:11, fontWeight:500, color:'#444', cursor:'pointer' }}>관리자</span>
					</div>
					<span style={{ fontSize:11, color:'#888' }}>Copyright © 2024 청년모아 All Rights Reserved</span>
				</div>
				<div style={{ display:'flex', gap:8, alignItems:'center', flexShrink:0 }}>
					{socials.map(s=><img key={s.l} src={s.src} alt={s.l} style={{ width:24, height:24, cursor:'pointer' }}/>)}
				</div>
			</div>
		</div>
	);
}

// ── HOME ──
function HomeScreen({ go, isLoggedIn }) {
	const [search, setSearch] = useState('');
	const [fav, setFav] = useState(new Set([1]));
	const [waitlist, setWaitlist] = useState(null);
	// 히어로 배경 로테이션 — 8초마다 다음 이미지로 크로스페이드
	const [heroIdx, setHeroIdx] = useState(0);
	useEffect(() => {
		const id = setInterval(() => setHeroIdx(i => (i + 1) % IMGS.banners.length), 8000);
		return () => clearInterval(id);
	}, []);
	return (
		<div className="screen-fade" style={{ background:T.bg }}>
			{/* Hero — extends behind sticky transparent header */}
			<div style={{ position:'relative', height:488+T.headerH, overflow:'hidden', marginTop:-T.headerH }}>
				{IMGS.banners.map((src, i) => (
					<img key={src} src={src} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', position:'absolute', inset:0, opacity:i===heroIdx?1:0, transition:'opacity 1.2s ease-in-out' }}/>
				))}
				{/* 브랜드 틴트 scrim — 사진이 비치되 인디고 톤 유지 */}
				<div style={{ position:'absolute', inset:0, background:'linear-gradient(135deg, oklch(0.30 0.16 280 / 0.80) 0%, oklch(0.42 0.16 280 / 0.62) 50%, oklch(0.55 0.13 280 / 0.48) 100%)' }}/>
				{/* 하단 darken scrim — 검색바·인기검색어 가독성 */}
				<div style={{ position:'absolute', inset:0, background:'linear-gradient(to bottom, transparent 30%, rgba(12,8,32,0.42) 100%)' }}/>
				<div style={{ position:'absolute', top:0, left:0, right:0, height:110, background:'linear-gradient(rgba(0,0,0,0.28),transparent)', zIndex:5 }}/>
				<div style={{ position:'absolute', top:T.headerH*2, left:0, right:0, bottom:0, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', textAlign:'center', padding:'0 40px', zIndex:2 }}>
					<div style={{ display:'inline-flex', padding:'6px 16px', borderRadius:20, background:'rgba(255,255,255,0.2)', backdropFilter:'blur(4px)', marginBottom:16 }}>
						<span style={{ color:'#fff', fontSize:14, fontWeight:500 }}>경기도 청년센터 프로그램 통합 플랫폼</span>
					</div>
					<h1 style={{ color:'#fff', fontSize:42, fontWeight:800, lineHeight:1.3, margin:0 }}>경기도 청년의 내일을<br/>함께 만들어갑니다</h1>
					<p style={{ color:'rgba(255,255,255,0.85)', fontSize:16, marginTop:12 }}>경기도 31개 시·군 청년센터의 프로그램을 한눈에 확인하세요</p>
					<div style={{ display:'flex', alignItems:'center', background:'#fff', borderRadius:T.tagR, padding:'0 6px 0 18px', height:52, width:460, marginTop:24, boxShadow:'0 4px 20px rgba(0,0,0,0.15)' }}>
						<Icon n="search" size={19} color="#94A3B8"/>
						<input value={search} onChange={e=>setSearch(e.target.value)} onKeyDown={e=>e.key==='Enter'&&go('search',{q:search})} placeholder="프로그램, 센터명으로 검색" style={{ flex:1, border:'none', outline:'none', fontSize:15, color:T.text, marginLeft:10, background:'transparent' }}/>
						<div className="btn-hover" onClick={()=>go('search',{q:search})} style={{ height:40, padding:'0 22px', background:T.primary, borderRadius:T.tagR, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
							<span style={{ color:'#fff', fontSize:14, fontWeight:600 }}>검색</span>
						</div>
					</div>
					{/* 인기 키워드 칩 */}
					<div style={{ display:'flex', alignItems:'center', gap:8, marginTop:16, flexWrap:'wrap', justifyContent:'center' }}>
						<span style={{ fontSize:12.5, color:'rgba(255,255,255,0.8)', fontWeight:600 }}>인기 검색어</span>
						{['취업 워크숍','창업 지원','심리 상담','주거 지원','자격증'].map(k=>(
							<div key={k} className="btn-hover" onClick={()=>go('search',{q:k})} style={{ padding:'5px 13px', borderRadius:T.tagR, background:'rgba(255,255,255,0.18)', backdropFilter:'blur(4px)', border:'1px solid rgba(255,255,255,0.25)', cursor:'pointer' }}>
								<span style={{ color:'#fff', fontSize:12.5, fontWeight:500 }}>{k}</span>
							</div>
						))}
					</div>
				</div>
			</div>
			{/* Stats */}
			<div style={{ display:'flex', justifyContent:'center', gap:48, padding:'28px 80px', background:T.surface, borderBottom:`1px solid ${T.borderLight}` }}>
				{[{n:'진행중 프로그램',v:'127'},{n:'참여 청년센터',v:'31'},{n:'누적 참여자',v:'15,420'}].map(s=>(
					<div key={s.n} style={{ display:'flex', alignItems:'center', gap:14 }}>
						<div style={{ width:44, height:44, borderRadius:12, background:T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
							<Icon n="check" size={22} color={T.primary}/>
						</div>
						<div>
							<div style={{ fontSize:22, fontWeight:700, color:T.text }}>{s.v}</div>
							<div style={{ fontSize:13, color:T.textSec }}>{s.n}</div>
						</div>
					</div>
				))}
			</div>
			{/* 퀵메뉴 그리드 */}
			<div style={{ maxWidth:1080, margin:'0 auto', padding:'28px 80px 4px', display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:14 }}>
				{[
					{icon:'search',label:'프로그램 찾기',screen:'programs'},
					{icon:'pin',label:'청년센터 찾기',screen:'centers'},
					{icon:'calendar',label:'내 신청 현황',screen:'mypage'},
					{icon:'bell',label:'공지사항',screen:'notices'},
				].map(q=>(
					<div key={q.label} className="card-hover" onClick={()=>go(q.screen)} style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:10, padding:'22px 12px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, cursor:'pointer' }}>
						<div style={{ width:48, height:48, borderRadius:14, background:T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
							<Icon n={q.icon} size={22} color={T.primary}/>
						</div>
						<span style={{ fontSize:14, fontWeight:600, color:T.text }}>{q.label}</span>
					</div>
				))}
			</div>
			{/* ④ 맞춤 추천 (로그인 시) */}
			{isLoggedIn && (
				<div style={{ padding:'40px 80px 8px' }}>
					<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:6 }}>
						<span style={{ fontSize:24, fontWeight:700, color:T.text }}>박시현님 맞춤 추천</span>
						<span style={{ padding:'3px 10px', borderRadius:T.tagR, background:T.primaryBg, color:T.primary, fontSize:12, fontWeight:600 }}>부천시 · 취업·창업 관심</span>
					</div>
					<div style={{ display:'flex', alignItems:'flex-end', justifyContent:'space-between', marginBottom:24 }}>
						<div style={{ fontSize:14, color:T.textSec }}>관심 지역과 분야를 바탕으로 골라드렸어요</div>
						<Btn variant="outline" size="s" onClick={()=>go('programs')}>전체보기</Btn>
					</div>
					<div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:20 }}>
						{[PROGRAMS[0],PROGRAMS[5],PROGRAMS[4],PROGRAMS[2]].map((pg,i)=>{ const c=capInfo(pg); return (
							<div key={pg.id} className="card-hover" onClick={()=>go('program-detail',{pg})} style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadowMd, border:`1px solid ${T.borderLight}` }}>
								<div style={{ width:'100%', height:150, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
									<img src={IMGS.pg[pg.id-1]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
									<div style={{ position:'absolute', top:10, left:10 }}><DdayChip pg={pg}/></div>
									<div style={{ position:'absolute', top:10, right:10, padding:'3px 8px', borderRadius:T.tagR, background:'rgba(255,255,255,0.92)', fontSize:10.5, fontWeight:600, color:T.primary }}>{i===0?'관심지역':'추천'}</div>
								</div>
								<div style={{ padding:'12px 14px 14px' }}>
									<div style={{ fontWeight:600, fontSize:14, color:T.text, marginBottom:4, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
									<div style={{ fontSize:12, color:T.textSec }}>{pg.center}</div>
								</div>
							</div>
						);})}
					</div>
				</div>
			)}
			{/* Programs — 비로그인 시에만(로그인 시는 맞춤 추천으로 대체) */}
			{!isLoggedIn && (
				<div style={{ padding:'44px 80px 40px' }}>
					<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:24 }}>
						<div>
							<div style={{ fontSize:24, fontWeight:700, color:T.text }}>프로그램</div>
							<div style={{ fontSize:14, color:T.textSec, marginTop:2 }}>진행중인 프로그램을 소개해드려요</div>
						</div>
						<Btn variant="outline" size="s" onClick={()=>go('programs')}>전체보기</Btn>
					</div>
					<div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:20 }}>
						{PROGRAMS.slice(0,4).map((pg,i)=>{ const c=capInfo(pg); return (
							<div key={pg.id} className="card-hover" onClick={()=>go('program-detail',{pg})} style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadowMd, border:`1px solid ${T.borderLight}` }}>
								<div style={{ width:'100%', height:170, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
									<img src={IMGS.pg[i%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', filter:c.full?'grayscale(0.5)':'none' }}/>
									<div style={{ position:'absolute', top:10, left:10 }}><DdayChip pg={pg}/></div>
									<div onClick={e=>{e.stopPropagation();setFav(f=>{const n=new Set(f);n.has(pg.id)?n.delete(pg.id):n.add(pg.id);return n;});}} style={{ position:'absolute', top:10, right:10, cursor:'pointer', zIndex:5 }}>
										<Icon n={fav.has(pg.id)?'starFill':'star'} size={20} color={fav.has(pg.id)?'#F59E0B':'rgba(255,255,255,0.9)'}/>
									</div>
								</div>
								<div style={{ padding:'12px 14px 14px' }}>
									<div style={{ fontWeight:600, fontSize:15, color:T.text, marginBottom:3, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
									<div style={{ fontSize:12, color:T.textSec, marginBottom:2 }}>{pg.center}</div>
									<div style={{ fontSize:12, color:T.textTri, marginBottom:10 }}>{pg.date}</div>
									<div style={{ marginBottom:10 }}><CapacityBar pg={pg}/></div>
									<div className="btn-hover" onClick={e=>{e.stopPropagation(); c.upcoming?go('program-detail',{pg}):c.full?setWaitlist(pg):go('program-detail',{pg});}} style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:5, height:34, border:`1px solid ${c.upcoming?T.secondary:c.full?T.border:T.primary}`, background:c.full?T.borderLight:'transparent', borderRadius:T.tagR, cursor:'pointer', fontSize:13, color:c.upcoming?T.secondary:c.full?T.textSec:T.primary, fontWeight:600 }}>
										<Icon n={(c.upcoming||c.full)?'bell':'check'} size={15} color={c.upcoming?T.secondary:c.full?T.textSec:T.primary}/>
										{c.upcoming?'오픈 알림 받기':c.full?'빈자리 알림 받기':'신청하기'}
									</div>
								</div>
							</div>
						);})}
					</div>
				</div>
			)}
			{/* Notices */}
			<div style={{ padding:'40px 80px 48px', background:T.primaryBg }}>
				<div style={{ display:'flex', alignItems:'flex-end', justifyContent:'space-between', marginBottom:24 }}>
					<div>
						<div style={{ fontSize:24, fontWeight:700, color:T.text }}>공지사항</div>
						<div style={{ fontSize:14, color:T.textSec, marginTop:2 }}>청년센터 소식을 전해드려요</div>
					</div>
					<Btn variant="outline" size="s" onClick={()=>go('notices')}>전체보기</Btn>
				</div>
				<div style={{ display:'flex', gap:24 }}>
					<div className="card-hover" onClick={()=>go('notice-detail',{notice:NOTICES[0]})} style={{ width:360, borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadowMd, flexShrink:0 }}>
						<div style={{ height:180, background:'#e5e7eb', overflow:'hidden' }}>
							<img src="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=360&h=220&fit=crop" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
						</div>
						<div style={{ padding:'16px 20px 20px' }}>
							<Badge text="📌 주요" variant="primary"/>
							<div style={{ fontWeight:700, fontSize:17, color:T.text, margin:'8px 0' }}>제1회 청년의 날 축제 안내</div>
							<div style={{ fontSize:13, color:T.textSec }}>2024.07.30 · 조회 756</div>
						</div>
					</div>
					<div style={{ flex:1, background:T.surface, borderRadius:T.radius, boxShadow:T.shadow, overflow:'hidden' }}>
						{NOTICES.slice(1).map((n,i)=>(
							<div key={n.id} onClick={()=>go('notice-detail',{notice:n})} className="card-hover" style={{ display:'flex', alignItems:'center', gap:14, padding:'0 24px', height:64, borderBottom:i<2?`1px solid ${T.borderLight}`:'none', cursor:'pointer' }}>
								<Badge text={n.cat} variant={n.cat==='공지'?'primary':n.cat==='운영'?'success':'muted'}/>
								<span style={{ flex:1, fontSize:14, fontWeight:500, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{n.title}</span>
								<span style={{ fontSize:12, color:T.textTri, flexShrink:0 }}>{n.date}</span>
							</div>
						))}
					</div>
				</div>
			</div>
			{/* Space Section */}
			<div style={{ padding:'40px 80px 48px' }}>
				<div style={{ display:'flex', alignItems:'flex-end', justifyContent:'space-between', marginBottom:24 }}>
					<div>
						<div style={{ fontSize:24, fontWeight:700, color:T.text }}>공간안내</div>
						<div style={{ fontSize:14, color:T.textSec, marginTop:2 }}>청년센터 공간을 소개해드려요</div>
					</div>
					<Btn variant="outline" size="s" icon="pin" onClick={()=>go('centers')}>지도에서 전체 센터 보기</Btn>
				</div>
				<div style={{ display:'flex', gap:20 }}>
					{IMGS.space.map((src,i)=>(
						<div key={i} className="card-hover" style={{ flex:1, height:240, borderRadius:T.radius, overflow:'hidden', position:'relative' }}>
							<img src={src} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
							<div style={{ position:'absolute', bottom:0, left:0, right:0, padding:'40px 18px 16px', background:'linear-gradient(transparent,rgba(0,0,0,0.6))' }}>
								<span style={{ color:'#fff', fontWeight:600, fontSize:15 }}>{['상상대로','내일스퀘어','비행지구'][i]}</span>
							</div>
						</div>
					))}
				</div>
			</div>
			{waitlist && <WaitlistModal pg={waitlist} onClose={()=>setWaitlist(null)}/>}
			<Footer/>
		</div>
	);
}

// ── PROGRAM LIST ──
function FilterPopChip({ label, options, sel, setSel }){
	const [open, setOpen] = useState(false);
	const count = options.filter(o=>sel.has(o)).length;
	return (
		<div style={{ position:'relative' }}>
			<div className="btn-hover" onClick={()=>setOpen(o=>!o)} style={{ display:'flex', alignItems:'center', gap:6, padding:'8px 14px', borderRadius:T.tagR, border:`1px solid ${count>0?T.primary:T.border}`, background:count>0?T.primaryBg:T.surface, cursor:'pointer' }}>
				<span style={{ fontSize:13, fontWeight:count>0?600:500, color:count>0?T.primary:T.textSec }}>{label}{count>0?` ${count}`:''}</span>
				<Icon n="chevD" size={13} color={count>0?T.primary:T.textTri}/>
			</div>
			{open && <>
				<div onClick={()=>setOpen(false)} style={{ position:'fixed', inset:0, zIndex:40 }}/>
				<div className="dropdown-enter" style={{ position:'absolute', top:'calc(100% + 6px)', left:0, zIndex:50, width:260, background:T.surface, borderRadius:12, border:`1px solid ${T.border}`, boxShadow:'0 12px 32px rgba(0,0,0,0.14)', padding:14 }}>
					{options.length>8 && <div style={{ display:'flex', alignItems:'center', gap:6, padding:'7px 10px', borderRadius:8, border:`1px solid ${T.border}`, marginBottom:10 }}>
						<Icon n="search" size={14} color={T.textTri}/>
						<span style={{ fontSize:12.5, color:T.textTri }}>{label} 검색</span>
					</div>}
					<div style={{ display:'flex', flexDirection:'column', gap:2, maxHeight:220, overflowY:'auto' }}>
						{options.map(o=>(
							<label key={o} onClick={()=>setSel(s=>{const n=new Set(s);n.has(o)?n.delete(o):n.add(o);return n;})} style={{ display:'flex', alignItems:'center', gap:8, padding:'7px 8px', borderRadius:6, cursor:'pointer' }}>
								<div style={{ width:16, height:16, borderRadius:4, border:`1.5px solid ${sel.has(o)?T.primary:T.border}`, background:sel.has(o)?T.primary:T.surface, display:'flex', alignItems:'center', justifyContent:'center' }}>
									{sel.has(o) && <svg viewBox="0 0 24 24" style={{width:10,height:10}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
								</div>
								<span style={{ fontSize:13, color:T.text }}>{o}</span>
							</label>
						))}
					</div>
				</div>
			</>}
		</div>
	);
}
function ProgramCalendar({ filtered, go }){
	const dayMap = {1:5,2:8,3:12,4:15,5:19,6:23};
	const firstDow=4, days=31; // 2024년 8월
	const byDay={};
	filtered.forEach(pg=>{ const d=dayMap[pg.id]||((pg.id*4)%28)+1; (byDay[d]=byDay[d]||[]).push(pg); });
	const [selDay, setSelDay] = useState(null);
	const TODAY = 19;
	const daySel = selDay ? (byDay[selDay]||[]) : [];
	const cells=[]; let day=1-firstDow;
	for(let i=0;i<35;i++){ cells.push(day>=1&&day<=days?day:null); day++; }
	const dows=['일','월','화','수','목','금','토'];
	return (
		<div style={{ display:'flex', gap:20, alignItems:'flex-start' }}>
			<div style={{ flex:1, minWidth:0, background:T.surface, borderRadius:T.radius, border:`1px solid ${T.border}`, padding:20, boxShadow:T.shadow }}>
				<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:16 }}>
					<div className="btn-hover" onClick={()=>setSelDay(TODAY)} style={{ display:'flex', alignItems:'center', gap:5, padding:'6px 14px', borderRadius:8, border:`1px solid ${T.border}`, cursor:'pointer' }}>
						<Icon n="calendar" size={13} color={T.textSec}/>
						<span style={{ fontSize:12.5, fontWeight:600, color:T.textSec }}>오늘</span>
					</div>
					<div style={{ display:'flex', alignItems:'center', gap:18 }}>
						<span className="btn-hover" style={{ fontSize:16, color:T.textTri, cursor:'pointer' }}>‹</span>
						<span style={{ fontSize:18, fontWeight:700, color:T.text }}>2024년 8월</span>
						<span className="btn-hover" style={{ fontSize:16, color:T.textTri, cursor:'pointer' }}>›</span>
					</div>
					<div style={{ width:62 }}/>
				</div>
				<div style={{ display:'grid', gridTemplateColumns:'repeat(7,1fr)', marginBottom:4 }}>
					{dows.map((d,i)=><div key={d} style={{ textAlign:'center', padding:'8px 0', fontSize:13, fontWeight:600, color:i===0?T.error:i===6?T.primary:T.textSec }}>{d}</div>)}
				</div>
				<div style={{ display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:4 }}>
					{cells.map((d,idx)=>{ const progs=d?(byDay[d]||[]):[]; const dow=idx%7; const isToday=d===TODAY; const isSel=d!=null&&d===selDay; return (
						<div key={idx} onClick={()=>d&&setSelDay(d)} className={d?'btn-hover':''} style={{ height:104, boxSizing:'border-box', borderRadius:8, border:`1px solid ${isSel?T.primary:T.borderLight}`, background:isSel?T.primaryBg:(d?T.surface:'transparent'), padding:6, overflow:'hidden', cursor:d?'pointer':'default', boxShadow:isSel?`0 0 0 1px ${T.primary}`:'none' }}>
							{d && <div style={{ display:'flex', alignItems:'center', gap:4, marginBottom:4 }}>
								<span style={{ fontSize:13, fontWeight:(isSel||isToday)?700:500, color:isSel?T.primary:isToday?T.primary:(dow===0?T.error:dow===6?T.primary:T.text), ...(isToday&&!isSel?{display:'inline-flex',alignItems:'center',justifyContent:'center',width:22,height:22,borderRadius:'50%',background:T.primary,color:'#fff',fontSize:12}:{}) }}>{d}</span>
								{isToday && !isSel && <span style={{ fontSize:10, color:T.primary, fontWeight:600 }}>오늘</span>}
							</div>}
							<div style={{ display:'flex', flexDirection:'column', gap:3 }}>
								{progs.slice(0,2).map(pg=>{ const c=capInfo(pg); return (
									<div key={pg.id} onClick={(e)=>{e.stopPropagation();go('program-detail',{pg});}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:4, padding:'2px 5px', borderRadius:4, background:T.borderLight, cursor:'pointer' }}>
										<span style={{ width:5, height:5, borderRadius:'50%', background:c.color, flexShrink:0 }}/>
										<span style={{ fontSize:10.5, color:T.text, fontWeight:500, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</span>
									</div>
								);})}
								{progs.length>2 && <span style={{ fontSize:10, color:T.textTri, paddingLeft:5 }}>+{progs.length-2}건 더</span>}
							</div>
						</div>
					);})}
				</div>
				<div style={{ display:'flex', gap:16, marginTop:16, flexWrap:'wrap' }}>
					{[['모집중',T.primary],['마감임박',T.error],['마감',T.textTri]].map(([l,col])=>(
						<div key={l} style={{ display:'flex', alignItems:'center', gap:5 }}><span style={{ width:8, height:8, borderRadius:'50%', background:col }}/><span style={{ fontSize:12, color:T.textSec }}>{l}</span></div>
					))}
				</div>
			</div>
			{/* 선택한 날짜 프로그램 — 날짜 클릭 시에만 우측 패널 슬라이드인 */}
			{selDay && <div style={{ width:320, flexShrink:0 }}>
				<>
					<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:12 }}>
						<div style={{ display:'flex', alignItems:'baseline', gap:8 }}>
							<span style={{ fontSize:16, fontWeight:700, color:T.text }}>8월 {selDay}일</span>
							<span style={{ fontSize:13, color:T.textSec }}>시작 {daySel.length}건</span>
						</div>
						<div className="btn-hover" onClick={()=>setSelDay(null)} style={{ width:26, height:26, borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}><Icon n="close" size={16} color={T.textTri}/></div>
					</div>
					{daySel.length===0
						? <div style={{ padding:'40px 0', textAlign:'center', color:T.textTri, fontSize:13, border:`1px dashed ${T.border}`, borderRadius:T.radius }}>이 날 시작하는<br/>프로그램이 없어요</div>
						: <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
							{daySel.map(pg=>{ const c=capInfo(pg); return (
								<div key={pg.id} onClick={()=>go('program-detail',{pg})} className="card-hover" style={{ display:'flex', gap:12, padding:12, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, cursor:'pointer' }}>
									<div style={{ width:60, height:60, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[(pg.id-1)%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
									<div style={{ flex:1, minWidth:0 }}>
										<div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:3 }}>
											<span style={{ width:7, height:7, borderRadius:'50%', background:c.color }}/>
											<span style={{ fontSize:11, color:T.textSec }}>{pg.center}</span>
											<DdayChip pg={pg}/>
										</div>
										<div style={{ fontSize:14, fontWeight:600, color:T.text, marginBottom:6, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
										<CapacityBar pg={pg} showLabel={false}/>
									</div>
								</div>
							);})}
						</div>}
				</>
			</div>}
		</div>
	);
}
function ProgramList({ go }) {
	const [sel, setSel] = useState(new Set());
	const [active, setActive] = useState('전체');
	const [sort, setSort] = useState('기본');
	const [fav, setFav] = useState(new Set([1]));
	const [waitlist, setWaitlist] = useState(null);
	const [view, setView] = useState('grid');
	const [loading, setLoading] = useState(true);
	useEffect(()=>{ const t=setTimeout(()=>setLoading(false), 700); return ()=>clearTimeout(t); }, []);
	let filtered = PROGRAMS.filter(p=>(active==='전체'||p.status===active) && (sel.size===0 || sel.has(p.region) || sel.has(p.center)));
	filtered = [...filtered].sort((a,b)=>{
		if(sort==='마감임박'){ const af=capInfo(a).full, bf=capInfo(b).full; if(af!==bf)return af?1:-1; return (a.dday??99)-(b.dday??99); }
		if(sort==='인기'){ return capInfo(b).ratio-capInfo(a).ratio; }
		return 0;
	});
	const regions = ['부천시','수원시','안양시','고양시','용인시'];
	const allRegions = ['수원시','성남시','고양시','용인시','부천시','안양시','안산시','화성시','남양주시','안성시','평택시','의정부시','파주시','시흥시','김포시','광명시','군포시','오산시'];
	const allCenters = ['내일스퀸어','상상대로','범계역 청년출구','원미청정구역','비행지구','오름','고천센터','딴딴회관','이루잡'];
	const centerSel = sel; // reuse for demo
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
			<div style={{ padding:'28px 0 24px', textAlign:'center', background:T.surface, borderBottom:`1px solid ${T.borderLight}` }}>
				<h2 style={{ fontSize:28, fontWeight:700, color:T.text }}>프로그램</h2>
			</div>
			<div style={{ maxWidth:1160, margin:'0 auto', padding:'28px 80px 48px' }}>
				{/* Main */}
				<div>
					<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:20, gap:12, flexWrap:'wrap' }}>
						<div style={{ display:'flex', alignItems:'center', gap:8, flexWrap:'wrap' }}>
							<div style={{ display:'flex', gap:6 }}>
								{['전체','진행중','진행예정','마감'].map(t=>(
									<div key={t} className="btn-hover" onClick={()=>setActive(t)} style={{ padding:'8px 16px', borderRadius:T.tagR, background:active===t?T.primary:'transparent', border:active===t?'none':`1px solid ${T.border}`, cursor:'pointer', fontSize:13, fontWeight:active===t?600:400, color:active===t?'#fff':T.textSec, whiteSpace:'nowrap' }}>
										{t}
									</div>
								))}
							</div>
							<div style={{ width:1, height:20, background:T.border }}/>
							<FilterPopChip label="지역" options={allRegions} sel={sel} setSel={setSel}/>
							<FilterPopChip label="청년센터" options={allCenters} sel={sel} setSel={setSel}/>
							{sel.size>0 && <div className="btn-hover" onClick={()=>setSel(new Set())} style={{ display:'flex', alignItems:'center', gap:4, padding:'8px 10px', cursor:'pointer' }}><Icon n="refresh" size={13} color={T.textTri}/><span style={{ fontSize:12.5, color:T.textTri }}>초기화</span></div>}
						</div>
						<div style={{ display:'flex', alignItems:'center', gap:14 }}>
							{view==='grid' && <>
								<span style={{ fontSize:13, color:T.textSec }}>전체 <strong style={{color:T.text}}>{filtered.length}</strong>건</span>
								<div style={{ display:'flex', gap:2, alignItems:'center' }}>
									{[['기본','기본순'],['마감임박','마감임박순'],['인기','인기순']].map(([k,label],si)=>(
										<React.Fragment key={k}>
											{si>0 && <span style={{ color:T.textTri, fontSize:11, margin:'0 2px' }}>·</span>}
											<span className="btn-hover" onClick={()=>setSort(k)} style={{ fontSize:13, color:sort===k?T.primary:T.textTri, fontWeight:sort===k?600:400, cursor:'pointer' }}>{label}</span>
										</React.Fragment>
									))}
								</div>
							</>}
							<div style={{ display:'flex', border:`1px solid ${T.border}`, borderRadius:8, overflow:'hidden' }}>
								{[['grid','목록'],['calendar','캘린더']].map(([v,label],vi)=>(
									<div key={v} className="btn-hover" onClick={()=>setView(v)} style={{ display:'flex', alignItems:'center', gap:5, padding:'7px 14px', background:view===v?T.primary:T.surface, borderLeft:vi>0?`1px solid ${T.border}`:'none', cursor:'pointer' }}>
										<Icon n={v} size={14} color={view===v?'#fff':T.textSec}/>
										<span style={{ fontSize:12.5, fontWeight:600, color:view===v?'#fff':T.textSec }}>{label}</span>
									</div>
								))}
							</div>
						</div>
					</div>
					{view==='calendar' && <ProgramCalendar filtered={filtered} go={go}/>}
					{view==='grid' && (
						<div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:16 }}>
							{loading
								? [0,1,2,3,4,5].map(i=><ProgramCardSkeleton key={i}/>)
								: filtered.map((pg,i)=>{ const c=capInfo(pg); return (
									<div key={pg.id} className="card-hover" onClick={()=>go('program-detail',{pg})} style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadow, border:`1px solid ${T.borderLight}` }}>
										<div style={{ width:'100%', height:160, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
											<img src={IMGS.pg[i%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', filter:c.full?'grayscale(0.5)':'none' }}/>
											<div style={{ position:'absolute', top:10, left:10 }}><DdayChip pg={pg}/></div>
											<div onClick={e=>{e.stopPropagation();setFav(f=>{const n=new Set(f);n.has(pg.id)?n.delete(pg.id):n.add(pg.id);return n;});}} style={{ position:'absolute', top:10, right:10, cursor:'pointer' }}>
												<Icon n={fav.has(pg.id)?'starFill':'star'} size={20} color={fav.has(pg.id)?'#F59E0B':'rgba(255,255,255,0.85)'}/>
											</div>
										</div>
										<div style={{ padding:'12px 14px 14px' }}>
											<div style={{ fontWeight:600, fontSize:15, color:T.text, marginBottom:3, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
											<div style={{ fontSize:12, color:T.textSec, marginBottom:2 }}>{pg.center}</div>
											<div style={{ fontSize:12, color:T.textTri, marginBottom:10 }}>{pg.date}</div>
											<div style={{ marginBottom:10 }}><CapacityBar pg={pg}/></div>
											{c.inactive ? (
												<div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:5, height:34, border:`1px solid ${T.border}`, background:T.borderLight, borderRadius:T.tagR, fontSize:13, color:T.textTri, fontWeight:600 }}>운영이 중단되었어요</div>
											) : (
												<div className="btn-hover" onClick={e=>{e.stopPropagation(); c.upcoming?go('program-detail',{pg}):c.full?setWaitlist(pg):go('program-detail',{pg});}} style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:5, height:34, border:`1px solid ${c.upcoming?T.secondary:c.full?T.border:T.primary}`, background:c.full?T.borderLight:'transparent', borderRadius:T.tagR, cursor:'pointer', fontSize:13, color:c.upcoming?T.secondary:c.full?T.textSec:T.primary, fontWeight:600 }}>
													<Icon n={(c.upcoming||c.full)?'bell':'check'} size={15} color={c.upcoming?T.secondary:c.full?T.textSec:T.primary}/>
													{c.upcoming?'오픈 알림 받기':c.full?'빈자리 알림 받기':'신청하기'}
												</div>
											)}
										</div>
									</div>
								);})}
						</div>
					)}
					{view==='grid' && (
						<div style={{ display:'flex', justifyContent:'center', alignItems:'center', gap:4, marginTop:28 }}>
							<div className="btn-hover" style={{ width:32, height:32, borderRadius:7, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', background:T.surface }}>
								<span style={{ fontSize:13, color:T.textTri }}>‹</span>
							</div>
							{[1,2,3,4,5].map(n=>(
								<div key={n} className="btn-hover" style={{ width:32, height:32, borderRadius:7, border:n===1?'none':`1px solid ${T.border}`, background:n===1?T.primary:T.surface, display:'flex', alignItems:'center', justifyContent:'center' }}>
									<span style={{ fontSize:13, fontWeight:n===1?700:500, color:n===1?'#fff':T.textSec, fontFamily:'Inter,sans-serif' }}>{n}</span>
								</div>
							))}
							<div className="btn-hover" style={{ width:32, height:32, borderRadius:7, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', background:T.surface }}>
								<span style={{ fontSize:13, color:T.textSec }}>›</span>
							</div>
						</div>
					)}
				</div>
			</div>
			{waitlist && <WaitlistModal pg={waitlist} onClose={()=>setWaitlist(null)}/>}
			<Footer/>
		</div>
	);
}

// ── PROGRAM DETAIL ──
function ProgramDetail({ go, pg, isLoggedIn, onLoginClick, addToast }) {
	const [fav, setFav] = useState(false);
	const [waitlist, setWaitlist] = useState(null);
	const [showMap, setShowMap] = useState(false);
	const c = capInfo(pg);
	const inactive = pg.inactive || pg.status==='중단';
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
			<div style={{ maxWidth:1000, margin:'0 auto', padding:'36px 0 48px' }}>
				<div className="btn-hover" onClick={()=>go('programs')} title="목록으로" style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:38, height:38, borderRadius:9, border:`1px solid ${T.border}`, background:T.surface, marginBottom:24, cursor:'pointer' }}>
					<Icon n="arrowL" size={18} color={T.textSec}/>
				</div>
				{inactive && (
					<div style={{ display:'flex', gap:12, padding:'16px 20px', borderRadius:T.radius, background:T.warningLight, border:`1px solid ${T.warning}33`, marginBottom:24 }}>
						<Icon n="bell" size={20} color={T.warning} style={{marginTop:1, flexShrink:0}}/>
						<div style={{ flex:1 }}>
							<div style={{ fontSize:15, fontWeight:700, color:T.text, marginBottom:3 }}>운영이 중단된 프로그램입니다</div>
							{pg.appStatus ? (
								<div style={{ fontSize:13.5, color:T.textSec, lineHeight:1.6 }}>
									운영 사정으로 해당 프로그램의 신규 신청이 중단되었어요. <strong style={{color:T.text}}>회원님의 신청 내역은 그대로 유효합니다.</strong> 변경 사항이 생기면 알림으로 안내드려요.
									<span className="btn-hover" onClick={()=>go('mypage',{tab:'history'})} style={{ display:'inline-flex', alignItems:'center', gap:2, marginLeft:8, color:T.primary, fontWeight:600, cursor:'pointer' }}>내 신청 현황 보기 <Icon n="chevR" size={13} color={T.primary}/></span>
								</div>
							) : (
								<div style={{ fontSize:13.5, color:T.textSec, lineHeight:1.6 }}>운영 사정으로 해당 프로그램의 신청이 중단되었어요. 이미 신청하셨다면 <strong style={{color:T.text}}>마이페이지 › 신청 현황</strong>에서 확인하실 수 있습니다. 문의는 운영 센터로 연락해주세요.</div>
							)}
						</div>
					</div>
				)}
				<div style={{ display:'flex', gap:32 }}>
					<div style={{ width:340, height:340, borderRadius:T.radius, overflow:'hidden', background:'#e5e7eb', flexShrink:0, position:'relative' }}>
						<img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', filter:inactive?'grayscale(0.7)':'none' }}/>
						{inactive && <div style={{ position:'absolute', inset:0, background:'rgba(0,0,0,0.35)', display:'flex', alignItems:'center', justifyContent:'center' }}><span style={{ padding:'6px 16px', borderRadius:T.tagR, background:'rgba(0,0,0,0.65)', color:'#fff', fontSize:14, fontWeight:600 }}>운영 중단</span></div>}
					</div>
					<div style={{ flex:1 }}>
						<div style={{ display:'flex', gap:8, marginBottom:8 }}>
							<Badge text={inactive?'운영중단':pg.status} variant={inactive?'muted':pg.status==='마감'?'muted':pg.status==='진행예정'?'secondary':'primary'}/>
						</div>
						<h1 style={{ fontSize:26, fontWeight:700, color:T.text, margin:'0 0 4px' }}>{pg.title}</h1>
						<p style={{ fontSize:14, color:T.textSec, margin:'0 0 16px' }}>{pg.center} · {pg.region}</p>
						<div style={{ padding:14, borderRadius:T.radius, background:(inactive||c.full)?T.borderLight:T.primaryBg, border:`1px solid ${(inactive||c.full)?T.border:T.primaryLight}`, marginBottom:20 }}>
							<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:8 }}>
								<span style={{ fontSize:14, fontWeight:700, color:inactive?T.textTri:c.color }}>{inactive?'모집 중단':c.upcoming?`신청 오픈까지 ${pg.openDday}일`:c.full?'모집 마감':c.label}{!inactive && !c.full && !c.upcoming && ` · 마감까지 ${pg.dday===0?'오늘':pg.dday+'일'}`}</span>
								<span style={{ fontSize:13, fontWeight:700, color:T.text }}>{c.applied}<span style={{ fontSize:12, fontWeight:400, color:T.textSec }}> / {pg.cap}명</span></span>
							</div>
							<CapacityBar pg={pg} showLabel={false}/>
							<div style={{ fontSize:12, color:T.textSec, marginTop:8 }}>{inactive?'운영 사정으로 모집이 중단되었습니다.':c.upcoming?`${pg.apply? pg.apply.replace('~',' ~ ').replace(/2024-/g,'') : ''} 신청 예정입니다. 오픈 알림을 신청하면 시작 시 알려드려요.`:c.full?'정원이 마감되었습니다. 알림을 신청하면 빈자리가 생길 시 알려드려요.':`현재 신청률 ${c.pct}% · 경쟁률 ${(c.applied/pg.cap).toFixed(1)}:1`}</div>
						</div>
						<div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:10 }}>
							{[['calendar','신청 기간',pg.apply? pg.apply.replace('~',' ~ ') : '2024-07-01 ~ 07-31'],['calendar','진행 기간',pg.date],['pin','진행 장소','__MAP__'],['user','모집인원',pg.cap+'명'],['bell','문의처','031-123-4567']].map(([ic,k,v])=>(
								<div key={k} style={{ display:'flex', alignItems:'flex-start', gap:11, padding:'14px 16px', borderRadius:T.radius, background:T.bg, border:`1px solid ${T.borderLight}` }}>
									<div style={{ width:34, height:34, borderRadius:9, background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}><Icon n={ic} size={16} color={T.primary}/></div>
									<div style={{ minWidth:0 }}>
										<div style={{ fontSize:12, color:T.textTri, marginBottom:3 }}>{k}</div>
										{v==='__MAP__' ? (
											<div style={{ display:'flex', alignItems:'center', gap:8, flexWrap:'wrap' }}>
												<span style={{ fontSize:13.5, fontWeight:600, color:T.text }}>{pg.center}</span>
												<div className="btn-hover" onClick={()=>setShowMap(true)} style={{ display:'inline-flex', alignItems:'center', gap:4, padding:'3px 9px', borderRadius:T.tagR, border:`1px solid ${T.primary}`, cursor:'pointer' }}>
													<Icon n="pin" size={12} color={T.primary}/><span style={{ fontSize:11.5, fontWeight:600, color:T.primary }}>지도</span>
												</div>
											</div>
										) : <span style={{ fontSize:13.5, fontWeight:600, color:T.text }}>{v}</span>}
									</div>
								</div>
							))}
						</div>
						<div style={{ display:'flex', gap:10, marginTop:20 }}>
							<div className="btn-hover" onClick={()=>{setFav(v=>!v); addToast(fav?'즐겨찾기 해제되었습니다.':'즐겨찾기에 추가되었습니다.');}} style={{ width:42, height:42, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
								<Icon n={fav?'starFill':'star'} size={20} color={fav?'#F59E0B':T.textSec}/>
							</div>
							<div className="btn-hover" onClick={()=>addToast('URL이 클립보드에 복사되었습니다.')} style={{ width:42, height:42, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
								<Icon n="share" size={18} color={T.textSec}/>
							</div>
							{inactive ? (
								<div style={{ flex:1, height:42, background:T.borderLight, borderRadius:T.tagR, display:'flex', alignItems:'center', justifyContent:'center', gap:6, cursor:'not-allowed' }}>
									<span style={{ color:T.textTri, fontSize:15, fontWeight:600 }}>신청이 중단된 프로그램입니다</span>
								</div>
							) : (
								<div className="btn-hover" onClick={()=>{ if(c.upcoming){addToast('오픈 알림을 신청했어요. 신청 시작 시 알려드릴게요.');return;} if(c.full){setWaitlist(pg);return;} if(!isLoggedIn){onLoginClick();return;} go('program-apply',{pg}); }} style={{ flex:1, height:42, background:(c.full||c.upcoming)?'transparent':T.primary, border:(c.full||c.upcoming)?`1.5px solid ${T.primary}`:'none', borderRadius:T.tagR, display:'flex', alignItems:'center', justifyContent:'center', gap:6, cursor:'pointer' }}>
									<Icon n={(c.full||c.upcoming)?'bell':'check'} size={18} color={(c.full||c.upcoming)?T.primary:'#fff'}/>
									<span style={{ color:(c.full||c.upcoming)?T.primary:'#fff', fontSize:15, fontWeight:(c.full||c.upcoming)?700:600 }}>{c.upcoming?'오픈 알림 받기':c.full?'빈자리 알림 받기':'신청하기'}</span>
								</div>
							)}
						</div>
					</div>
				</div>
				<div style={{ height:1, background:T.border, margin:'32px 0' }}/>
				<h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:16 }}>지원 대상</h3>
				<div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:12, marginBottom:16 }}>
					{[
						{ icon:'user', k:'연령', v:'만 19세 ~ 39세 청년' },
						{ icon:'pin', k:'거주지', v:'경기도 거주 또는 활동 중인 청년' },
						{ icon:'calendar', k:'기타', v:'전 회차 참석 가능자 우대' },
					].map(it=>(
						<div key={it.k} style={{ display:'flex', gap:12, padding:'14px 16px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}` }}>
							<div style={{ width:36, height:36, borderRadius:9, background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
								<Icon n={it.icon} size={18} color={T.primary}/>
							</div>
							<div>
								<div style={{ fontSize:12, color:T.textTri, marginBottom:2 }}>{it.k}</div>
								<div style={{ fontSize:14, fontWeight:600, color:T.text }}>{it.v}</div>
							</div>
						</div>
					))}
				</div>
				<div style={{ display:'flex', gap:8, padding:'12px 14px', borderRadius:T.radius, background:T.warningLight, marginBottom:8 }}>
					<Icon n="bell" size={16} color={T.warning} style={{marginTop:1}}/>
					<span style={{ fontSize:13, color:T.text, lineHeight:1.55 }}>신청 전 <strong>지원 대상을 반드시 확인</strong>해주세요. 요건 미충족 시 프로그램 신청이 반려될 수 있습니다.</span>
				</div>

				<div style={{ height:1, background:T.border, margin:'32px 0' }}/>
				<h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:16 }}>프로그램 설명</h3>
				<div style={{ width:'100%', height:360, borderRadius:T.radius, background:'#E5E7EB', display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden', position:'relative' }}>
					<img src={IMGS.pg[1]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', opacity:0.3 }}/>
					<span style={{ position:'absolute', fontSize:14, color:'#888', fontWeight:500 }}>프로그램 상세 설명 콘텐츠 영역</span>
				</div>
			</div>
			{waitlist && <WaitlistModal pg={waitlist} onClose={()=>setWaitlist(null)} addToast={addToast}/>}
			{showMap && (
				<ModalCard title="진행 장소" width={520} onClose={()=>setShowMap(false)}>
					<div style={{ margin:'-20px -22px 0' }}>
						{/* 지도 플레이스홀더 */}
						<div style={{ position:'relative', height:280, background:'#E8EBF0', display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden' }}>
							<div style={{ position:'absolute', inset:0, backgroundImage:`repeating-linear-gradient(0deg, transparent, transparent 38px, ${T.border} 38px, ${T.border} 39px), repeating-linear-gradient(90deg, transparent, transparent 38px, ${T.border} 38px, ${T.border} 39px)`, opacity:0.5 }}/>
							<div style={{ position:'relative', display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
								<div style={{ width:44, height:44, borderRadius:'50% 50% 50% 0', background:T.primary, transform:'rotate(-45deg)', display:'flex', alignItems:'center', justifyContent:'center', boxShadow:'0 4px 12px rgba(0,0,0,0.2)' }}>
									<div style={{ transform:'rotate(45deg)' }}><Icon n="pin" size={20} color="#fff"/></div>
								</div>
								<span style={{ position:'relative', top:6, fontSize:13, fontWeight:700, color:T.text, background:'rgba(255,255,255,0.9)', padding:'3px 10px', borderRadius:T.tagR }}>{pg.center}</span>
							</div>
							<span style={{ position:'absolute', bottom:10, right:12, fontSize:11, color:T.textTri }}>지도 이미지 (카카오/네이버 지도 SDK 연동)</span>
						</div>
						<div style={{ padding:'18px 22px' }}>
							<div style={{ fontSize:15, fontWeight:600, color:T.text, marginBottom:4 }}>{pg.center}</div>
							<div style={{ fontSize:13.5, color:T.textSec, marginBottom:16 }}>경기도 {pg.region} 청년로 123, 3층</div>
							<div style={{ display:'flex', gap:10 }}>
								<div className="btn-hover" onClick={()=>addToast('주소가 복사되었습니다.')} style={{ flex:1, height:44, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', gap:6, cursor:'pointer' }}><Icon n="share" size={16} color={T.textSec}/><span style={{ fontSize:13.5, fontWeight:600, color:T.textSec }}>주소 복사</span></div>
								<div className="btn-hover" onClick={()=>addToast('길찾기 앱으로 연결됩니다.')} style={{ flex:1, height:44, borderRadius:8, background:T.primary, display:'flex', alignItems:'center', justifyContent:'center', gap:6, cursor:'pointer' }}><Icon n="pin" size={16} color="#fff"/><span style={{ fontSize:13.5, fontWeight:600, color:'#fff' }}>길찾기</span></div>
							</div>
						</div>
					</div>
				</ModalCard>
			)}
			{/* ── 하단 고정 CTA 바 (Sticky) ── */}
			<div style={{ position:'sticky', bottom:0, zIndex:20, background:'rgba(255,255,255,0.92)', backdropFilter:'blur(8px)', borderTop:`1px solid ${T.borderLight}`, boxShadow:'0 -4px 20px rgba(0,0,0,0.06)' }}>
				<div style={{ maxWidth:1080, margin:'0 auto', padding:'14px 24px', display:'flex', alignItems:'center', gap:16 }}>
					<div style={{ flex:1, minWidth:0 }}>
						<div style={{ fontSize:15, fontWeight:700, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
						<div style={{ fontSize:12.5, color:inactive?T.textTri:c.full?T.textTri:T.primary, fontWeight:600 }}>{inactive?'모집 중단':c.upcoming?`신청 오픈까지 ${pg.openDday}일`:c.full?'모집 마감':`마감까지 ${pg.dday===0?'오늘':pg.dday+'일'} · ${c.applied}/${pg.cap}명`}</div>
					</div>
					<div onClick={()=>setFav(v=>!v)} className="btn-hover" style={{ width:48, height:48, flexShrink:0, borderRadius:12, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
						<Icon n={fav?'starFill':'star'} size={22} color={fav?'#F59E0B':T.textSec}/>
					</div>
					{inactive ? (
						<div style={{ minWidth:200, height:48, background:T.borderLight, borderRadius:12, display:'flex', alignItems:'center', justifyContent:'center', cursor:'not-allowed' }}>
							<span style={{ color:T.textTri, fontSize:15, fontWeight:700 }}>신청이 중단되었습니다</span>
						</div>
					) : (
						<div className="btn-hover" onClick={()=>{ if(c.upcoming){addToast('오픈 알림을 신청했어요. 신청 시작 시 알려드릴게요.');return;} if(c.full){setWaitlist(pg);return;} if(!isLoggedIn){onLoginClick();return;} go('program-apply',{pg}); }} style={{ minWidth:200, height:48, background:(c.full||c.upcoming)?'transparent':T.primary, border:(c.full||c.upcoming)?`1.5px solid ${T.primary}`:'none', borderRadius:12, display:'flex', alignItems:'center', justifyContent:'center', gap:7, cursor:'pointer' }}>
							<Icon n={(c.full||c.upcoming)?'bell':'check'} size={19} color={(c.full||c.upcoming)?T.primary:'#fff'}/>
							<span style={{ color:(c.full||c.upcoming)?T.primary:'#fff', fontSize:16, fontWeight:700 }}>{c.upcoming?'오픈 알림 받기':c.full?'빈자리 알림 받기':'신청하기'}</span>
						</div>
					)}
				</div>
			</div>
			<Footer/>
		</div>
	);
}
function ProgramApply({ go, pg, addToast }) {
	const [agreed, setAgreed] = useState(false);
	const [reason, setReason] = useState('');
	const [step, setStep] = useState(1);
	const STEPS = ['신청자 정보','추가 정보','약관 동의'];
	const handleSubmit = () => {
		if(!agreed) { addToast('개인정보 수집 동의가 필요합니다.'); return; }
		go('apply-complete', {pg});
	};
	const next = () => setStep(s=>Math.min(3,s+1));
	const prev = () => step>1 ? setStep(s=>s-1) : go('program-detail',{pg});
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
			<div style={{ maxWidth:700, margin:'0 auto', padding:'36px 0 48px' }}>
				<div className="btn-hover" onClick={prev} title={step>1?'이전 단계':'이전으로'} style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:38, height:38, borderRadius:9, border:`1px solid ${T.border}`, background:T.surface, marginBottom:20, cursor:'pointer' }}>
					<Icon n="arrowL" size={18} color={T.textSec}/>
				</div>
				<h2 style={{ fontSize:26, fontWeight:700, color:T.text, textAlign:'center', marginBottom:24 }}>프로그램 신청</h2>
				{/* 스텝 프로그레스 */}
				<div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:0, marginBottom:28 }}>
					{STEPS.map((label,i)=>{ const n=i+1; const done=step>n; const active=step===n; return (
						<React.Fragment key={label}>
							<div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:7 }}>
								<div style={{ width:34, height:34, borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', background:(done||active)?T.primary:T.borderLight, color:(done||active)?'#fff':T.textTri, fontSize:14, fontWeight:700, transition:'all 200ms' }}>
									{done ? <Icon n="check" size={16} color="#fff"/> : n}
								</div>
								<span style={{ fontSize:12.5, fontWeight:active?700:500, color:active?T.primary:T.textTri }}>{label}</span>
							</div>
							{n<3 && <div style={{ flex:1, maxWidth:80, height:2, background:step>n?T.primary:T.border, margin:'0 6px', marginBottom:22 }}/>}
						</React.Fragment>
					);})}
				</div>
				{/* Summary (항상 노출) */}
				<div style={{ display:'flex', gap:16, padding:16, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, marginBottom:20 }}>
					<div style={{ width:64, height:64, borderRadius:10, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
					<div><Badge text="진행중"/><div style={{ fontWeight:600, fontSize:15, color:T.text, margin:'6px 0 2px' }}>{pg.title}</div><div style={{ fontSize:12.5, color:T.textSec }}>{pg.center} · {pg.date}</div></div>
				</div>
				<div style={{ background:T.surface, borderRadius:T.radius, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, padding:'24px 26px', marginBottom:20 }}>
					{step===1 && (
						<div>
							<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:16 }}>
								<span style={{ fontWeight:700, fontSize:16, color:T.text }}>신청자 정보</span>
								<span style={{ padding:'2px 9px', borderRadius:T.tagR, background:T.primaryBg, color:T.primary, fontSize:11.5, fontWeight:600 }}>회원정보 자동입력</span>
							</div>
							<div style={{ display:'flex', flexDirection:'column', gap:14 }}>
								{[{label:'이름',val:'박시현'},{label:'핸드폰 번호',val:'010-1234-5678'},{label:'이메일',val:'hyuuun0321@naver.com',note:'이메일을 통해 프로그램 관련 알림을 드립니다.'}].map(f=>(
									<div key={f.label}>
										<span style={{ fontSize:13, fontWeight:500, color:T.textSec, marginBottom:6, display:'block' }}>{f.label}</span>
										<div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>{f.val}</div>
										{f.note && <div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>{f.note}</div>}
									</div>
								))}
							</div>
							<div style={{ fontSize:12.5, color:T.textTri, marginTop:12 }}>정보 수정은 마이페이지 › 개인 정보 수정에서 가능합니다.</div>
						</div>
					)}
					{step===2 && (
						<div>
							<div style={{ fontWeight:700, fontSize:16, color:T.text, marginBottom:16 }}>추가 정보</div>
							<span style={{ fontSize:13, fontWeight:500, color:T.textSec, marginBottom:6, display:'block' }}>지원 동기</span>
							<textarea value={reason} onChange={e=>setReason(e.target.value)} placeholder="지원 동기를 입력해주세요. (선택)" style={{ width:'100%', height:120, borderRadius:8, border:`1.5px solid ${reason?T.primary:T.border}`, padding:'12px 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, resize:'none', outline:'none', transition:'border 150ms', boxSizing:'border-box' }}/>
						</div>
					)}
					{step===3 && (
						<div>
							<div style={{ fontWeight:700, fontSize:16, color:T.text, marginBottom:16 }}>개인정보 수집 동의</div>
							<div style={{ padding:14, borderRadius:8, border:`1px solid ${T.border}`, background:T.bg, fontSize:13, color:T.textSec, lineHeight:1.7, maxHeight:160, overflow:'auto', marginBottom:14 }}>
								개인정보수집 및 초상 이용<br/><br/>1. 개인정보 수집 항목: 성명, 생년월일, 성별, 연락처<br/>2. 이용목적: 강좌신청 예약 접수 및 홍보 목적<br/>3. 보유기간: 프로그램 종료 후 3년<br/>4. 동의를 거부할 권리가 있으며, 거부 시 신청이 제한됩니다.
							</div>
							<label onClick={()=>setAgreed(v=>!v)} style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer', userSelect:'none' }}>
								<div style={{ width:20, height:20, borderRadius:'50%', border:`2px solid ${agreed?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms' }}>
									{agreed && <div style={{ width:10, height:10, borderRadius:'50%', background:T.primary }}/>}
								</div>
								<span style={{ fontSize:14, color:T.text }}>개인정보 수집 및 이용에 동의합니다</span>
							</label>
						</div>
					)}
				</div>
				{/* 네비게이션 */}
				<div style={{ display:'flex', gap:10 }}>
					{step>1 && <Btn size="l" variant="secondary" onClick={prev} style={{ flex:1 }}>이전</Btn>}
					{step<3
						? <Btn size="l" variant="primary" onClick={next} style={{ flex:2 }}>다음</Btn>
						: <Btn size="l" variant="primary" onClick={handleSubmit} disabled={!agreed} style={{ flex:2 }}>신청 완료</Btn>}
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── APPLY COMPLETE ──
function ApplyComplete({ go, pg }) {
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'56px 80px' }}>
				<div style={{ width:88, height:88, borderRadius:'50%', background:'#D1FAE5', display:'flex', alignItems:'center', justifyContent:'center', marginBottom:24 }}>
					<svg viewBox="0 0 24 24" style={{ width:46, height:46 }}>
						<circle cx="12" cy="12" r="10" fill="#10B981"/>
						<path d="M8 12.5l2.5 2.5L16 9.5" stroke="#fff" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
					</svg>
				</div>
				<h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:8 }}>프로그램 신청이 완료되었습니다</h2>
				<p style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, marginBottom:28, wordBreak:'keep-all' }}>
					신청하신 내용은 <strong style={{color:T.text}}>승인 대기</strong> 상태입니다.<br/>
					결과는 이메일과 마이페이지 &gt; 프로그램 신청 현황에서 확인하세요.
				</p>
				<div style={{ width:520, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, padding:20, display:'flex', gap:16, marginBottom:28 }}>
					<div style={{ width:80, height:80, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
					<div style={{ flex:1 }}>
						<span style={{ padding:'2px 10px', borderRadius:T.tagR, background:T.warningLight, color:T.warning, fontSize:12, fontWeight:600 }}>승인 대기</span>
						<div style={{ fontWeight:600, fontSize:16, color:T.text, margin:'6px 0 2px' }}>{pg.title}</div>
						<div style={{ fontSize:13, color:T.textSec }}>{pg.center} · {pg.date}</div>
						<div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>신청일시 2024-07-05 17:11</div>
					</div>
				</div>
				<div style={{ display:'flex', gap:12 }}>
					<Btn size="l" variant="ghost" onClick={()=>go('home')} style={{width:160}}>홈으로</Btn>
					<Btn size="l" variant="primary" onClick={()=>go('mypage')} style={{width:200}}>신청 현황 보기</Btn>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── MYPAGE ──
function MyPage({ go, addToast, initialTab }) {
	const [tab, setTab] = useState(initialTab||'history');
	const [showCancel, setShowCancel] = useState(false);
	const [cancelReason, setCancelReason] = useState('');
	const [cancelEtc, setCancelEtc] = useState('');
	const [cancelError, setCancelError] = useState(false);
	const [noti, setNoti] = useState({ kakao:true, sms:false, email:true, remind:true, empty:true, news:false });
	const [applications, setApplications] = useState([
		{ ...PROGRAMS[1], appStatus:'승인', appliedAt:'2024.07.05 17:11:31' },
		{ ...PROGRAMS[0], appStatus:'대기', appliedAt:'2024.07.03 17:11:31' },
		{ ...PROGRAMS[2], appStatus:'반려', appliedAt:'2024.07.02 17:11:31' },
		{ ...PROGRAMS[3], appStatus:'취소', appliedAt:'2024.07.01 17:11:31' },
		{ ...PROGRAMS[4], status:'중단', appStatus:'승인', appliedAt:'2024.06.20 10:02:11' },
	]);
	const [period, setPeriod] = useState('3개월');
	const [statusFilter, setStatusFilter] = useState('전체');
	// 상태 뱃지: 승인=파랑 · 대기=검정 · 반려=빨강 · 취소=회색 (기획서 기준)
	const appBadge = { '승인':{bg:T.successLight,c:T.success}, '대기':{bg:T.warningLight,c:T.warning}, '반려':{bg:'#FEF2F2',c:T.error}, '취소':{bg:T.borderLight,c:T.textTri} }; // 파스텔 통일안(확정 전) — 기획서 솔리드안: 승인=파랑·대기=검정·반려=빨강·취소=회색
	const doCancel = () => {
		if(!cancelReason || (cancelReason==='기타' && !cancelEtc.trim())){setCancelError(true);return;}
		setApplications(apps=>apps.filter(a=>a.id!==1));
		setShowCancel(false);
		addToast('취소되었습니다.');
	};
	// ⑧ 신청 상태 뱃지 매핑 통일: 대기=warning · 승인=success · 반려=error · 취소=muted
	const statusStyle = { '대기':{bg:T.warningLight,c:T.warning}, '승인':{bg:T.successLight,c:T.success}, '반려':{bg:'#FEF2F2',c:T.error}, '취소':{bg:T.borderLight,c:T.textTri}, '운영중단':{bg:T.borderLight,c:T.textTri} };
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ padding:'36px 80px 56px', flex:1, width:'100%', maxWidth:1080, margin:'0 auto', boxSizing:'border-box' }}>
				{/* ── 프로필 요약 카드 (A안: 대시보드형) ── */}
				<div style={{ display:'flex', alignItems:'center', gap:20, padding:'26px 30px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, marginBottom:20 }}>
					<Avatar size={58}/>
					<div style={{ flex:1, minWidth:0 }}>
						<div style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:3 }}>반가워요, 박시현님!</div>
						<div style={{ fontSize:13, color:T.textSec, marginBottom:9 }}>hyuuun0321@naver.com</div>
						<div style={{ display:'flex', gap:6 }}>
							{['관심 지역 · 수원시','관심 · 취업·창업'].map(t=>(
								<span key={t} style={{ padding:'3px 10px', borderRadius:T.tagR, background:T.primaryLight, color:T.primary, fontSize:12, fontWeight:600 }}>{t}</span>
							))}
						</div>
					</div>
					{/* KPI */}
					<div style={{ display:'flex' }}>
						{[
							{label:'진행중인 신청',val:applications.filter(a=>a.appStatus==='승인'||a.appStatus==='대기').length,goTab:'history'},
							{label:'종료된 신청',val:applications.filter(a=>a.appStatus==='반려'||a.appStatus==='취소').length,goTab:'history'},
							{label:'즐겨찾기',val:0,goTab:'favorites'},
						].map((s,i)=>(
							<div key={s.label} className="btn-hover" onClick={()=>setTab(s.goTab)} style={{ textAlign:'center', padding:'8px 26px', borderLeft:i>0?`1px solid ${T.borderLight}`:'none', cursor:'pointer' }}>
								<div style={{ fontSize:24, fontWeight:800, color:T.primary, lineHeight:1.2 }}>{s.val}</div>
								<div style={{ fontSize:12, color:T.textSec, marginTop:2 }}>{s.label}</div>
							</div>
						))}
					</div>
				</div>
				{/* ── 탭 바 (세그먼트형 — 헤더/카드 하단선과 라인 중복 제거) ── */}
				<div style={{ display:'flex', gap:4, padding:5, background:T.surface, border:`1px solid ${T.borderLight}`, borderRadius:12, marginBottom:24, boxShadow:T.shadow }}>
					{[{key:'history',icon:'calendar',label:'신청 현황'},{key:'favorites',icon:'star',label:'즐겨찾기'},{key:'noti',icon:'bell',label:'알림 설정'},{key:'profile',icon:'user',label:'개인정보 수정'}].map(m=>(
						<div key={m.key} onClick={()=>setTab(m.key)} className="btn-hover" style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', gap:7, padding:'11px 12px', cursor:'pointer', borderRadius:8, background:tab===m.key?T.primaryLight:'transparent' }}>
							<Icon n={m.icon} size={16} color={tab===m.key?T.primary:T.textSec}/>
							<span style={{ fontSize:14, fontWeight:tab===m.key?700:500, color:tab===m.key?T.primary:T.textSec }}>{m.label}</span>
						</div>
					))}
				</div>
				{/* Content (흰 라운드 카드 그룹) */}
				<div style={{ background:T.surface, borderRadius:T.radius, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow, padding:'28px 30px' }}>
					{tab==='history' && (
						<div>
							<h3 style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:6 }}>프로그램 신청 내역</h3>
							<div style={{ fontSize:13, color:T.textSec, marginBottom:18 }}>최대 지난 3년간의 프로그램 신청 내역까지 확인할 수 있어요</div>
							{/* 기간 필터 */}
							<div style={{ display:'flex', gap:8, marginBottom:14 }}>
								{['3개월','6개월','1년','3년'].map(p=>(
									<div key={p} className="btn-hover" onClick={()=>setPeriod(p)} style={{ flex:1, height:44, borderRadius:8, border:`1px solid ${period===p?T.primary:T.border}`, background:period===p?T.primaryBg:'transparent', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', fontSize:14, fontWeight:period===p?700:400, color:period===p?T.primary:T.textSec }}>{p}</div>
								))}
							</div>
							{/* 상태 필터 칩 */}
							<div style={{ display:'flex', gap:7, marginBottom:20, flexWrap:'wrap' }}>
								{['전체','승인','대기','반려','취소'].map(st=>{
									const cnt = st==='전체'?applications.length:applications.filter(a=>a.appStatus===st).length;
									const on = statusFilter===st;
									return (
										<div key={st} className="btn-hover" onClick={()=>setStatusFilter(st)} style={{ display:'flex', alignItems:'center', gap:5, padding:'6px 13px', borderRadius:T.tagR, border:`1px solid ${on?T.primary:T.border}`, background:on?T.primary:'transparent', cursor:'pointer' }}>
											<span style={{ fontSize:13, fontWeight:on?600:500, color:on?'#fff':T.textSec }}>{st}</span>
											<span style={{ fontSize:12, fontWeight:600, color:on?'rgba(255,255,255,0.85)':T.textTri }}>{cnt}</span>
										</div>
									);
								})}
							</div>
							{(() => { const shown = statusFilter==='전체'?applications:applications.filter(a=>a.appStatus===statusFilter); return (
								shown.length===0
									? <div style={{ display:'flex', flexDirection:'column', alignItems:'center', padding:'80px 0', gap:14 }}>
										<Icon n="calendar" size={64} color={T.border}/>
										<span style={{ fontSize:15, color:T.textSec }}>{statusFilter==='전체'?'신청한 프로그램이 없습니다.':`'${statusFilter}' 상태의 신청 내역이 없습니다.`}</span>
										{statusFilter==='전체' && <Btn size="m" onClick={()=>go('programs')}>프로그램 보기</Btn>}
									</div>
									: <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
										{shown.map((app,i)=>{
											const b = appBadge[app.appStatus]||appBadge['취소'];
											const canCancel = app.appStatus==='승인' || app.appStatus==='대기';
											const canReapply = app.appStatus==='반려' || app.appStatus==='취소';
											return (
												<div key={app.id} style={{ padding:'18px 20px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, boxShadow:T.shadow }}>
													{/* 상단: 신청일시 + 신청 상세 */}
													<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:14 }}>
														<span style={{ fontSize:13, color:T.textSec, fontFamily:'Inter,Pretendard,sans-serif' }}>{app.appliedAt}</span>
														<div className="btn-hover" onClick={()=>go('application-detail',{pg:app})} style={{ display:'flex', alignItems:'center', gap:2, cursor:'pointer' }}>
															<span style={{ fontSize:13, color:T.textSec }}>신청 상세</span>
															<Icon n="chevR" size={15} color={T.textSec}/>
														</div>
													</div>
													<div style={{ display:'flex', gap:16 }}>
														<div style={{ width:88, height:88, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[i%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
														<div style={{ flex:1, minWidth:0 }}>
															<div className="btn-hover" onClick={()=>go('program-detail',{pg:app})} style={{ display:'flex', alignItems:'center', gap:8, marginBottom:6, cursor:'pointer' }}>
																<span style={{ fontWeight:600, fontSize:16, color:T.text }}>{app.title}</span>
																<span style={{ padding:'2px 10px', borderRadius:T.tagR, background:b.bg, color:b.c, fontSize:12, fontWeight:600 }}>{app.appStatus}</span>
																{app.status==='중단' && <span style={{ padding:'2px 10px', borderRadius:T.tagR, background:T.borderLight, color:T.textTri, fontSize:12, fontWeight:600 }}>운영중단</span>}
															</div>
															<div style={{ fontSize:13, color:T.textSec, marginBottom:12 }}>{app.center} · {app.date}</div>
															{/* 액션 버튼 */}
															{canCancel && (
																<Btn variant="dangerOutline" size="m" fullWidth onClick={()=>setShowCancel(true)}>신청 취소</Btn>
															)}
															{canReapply && (
																<div style={{ display:'flex', gap:8 }}>
																	<Btn variant="secondary" size="m" onClick={()=>go('program-detail',{pg:app})} style={{ flex:1 }}>재신청</Btn>
																	<Btn variant="ghost" size="m" disabled style={{ flex:1 }}>신청 취소</Btn>
																</div>
															)}
														</div>
													</div>
												</div>
											);
										})}
									</div>
							); })()}
						</div>
					)}
					{tab==='favorites' && (
						<div>
							<h3 style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:18 }}>즐겨찾기한 프로그램</h3>
							<div style={{ display:'flex', flexDirection:'column', alignItems:'center', padding:'80px 0', gap:14 }}>
								<Icon n="star" size={64} color={T.border}/>
								<span style={{ fontSize:15, color:T.textSec }}>즐겨찾기한 프로그램이 없습니다.</span>
								<Btn size="m" onClick={()=>go('programs')}>프로그램 보기</Btn>
							</div>
						</div>
					)}
					{tab==='profile' && (
						<div>
							<h3 style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:6 }}>개인 정보 수정</h3>
							<div style={{ fontSize:14, fontWeight:600, color:T.text, marginBottom:16 }}>비밀번호 재확인</div>
							<p style={{ fontSize:14, color:T.textSec, marginBottom:20 }}>회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.</p>
							<div style={{ maxWidth:480 }}>
								<div style={{ display:'grid', gridTemplateColumns:'90px 1fr', gap:'14px 16px', alignItems:'center', marginBottom:20 }}>
									<span style={{ fontSize:14, fontWeight:500, color:T.text }}>아이디</span>
									<div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>hyuuun0321</div>
									<span style={{ fontSize:14, fontWeight:500, color:T.text }}>비밀번호 *</span>
									<input type="password" placeholder="비밀번호를 입력해주세요." style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none' }}/>
								</div>
								<div style={{ display:'flex', justifyContent:'center' }}>
									<Btn size="m" onClick={()=>addToast('확인되었습니다.')}>확인</Btn>
								</div>
							</div>
						</div>
					)}
					{tab==='noti' && (
						<div>
							<h3 style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:24 }}>알림 설정</h3>
							<div style={{ fontSize:16, fontWeight:700, color:T.text, marginBottom:4 }}>알림 받을 방법</div>
							<div style={{ fontSize:13, color:T.textSec, marginBottom:14 }}>여러 방법을 동시에 선택할 수 있어요</div>
							<div style={{ display:'flex', flexDirection:'column', gap:10, marginBottom:32, maxWidth:560 }}>
								{[{k:'kakao',label:'카카오 알림톡',sub:'010-1234-5678'},{k:'sms',label:'문자(SMS)',sub:'010-1234-5678'},{k:'email',label:'이메일',sub:'hyuuun0321@naver.com'}].map(cc=>(
									<div key={cc.k} style={{ display:'flex', alignItems:'center', gap:14, padding:'14px 18px', borderRadius:T.radius, background:T.surface, border:`1px solid ${noti[cc.k]?T.primaryLight:T.border}` }}>
										<div style={{ width:38, height:38, borderRadius:10, background:noti[cc.k]?T.primaryLight:T.borderLight, display:'flex', alignItems:'center', justifyContent:'center' }}><Icon n="bell" size={18} color={noti[cc.k]?T.primary:T.textSec}/></div>
										<div style={{ flex:1 }}><div style={{ fontSize:14.5, fontWeight:600, color:T.text }}>{cc.label}</div><div style={{ fontSize:12.5, color:T.textTri }}>{cc.sub}</div></div>
										<Toggle on={noti[cc.k]} onClick={()=>setNoti(s=>({...s,[cc.k]:!s[cc.k]}))}/>
									</div>
								))}
							</div>
							<div style={{ fontSize:16, fontWeight:700, color:T.text, marginBottom:4 }}>알림 항목</div>
							<div style={{ fontSize:13, color:T.textSec, marginBottom:14 }}>받고 싶은 알림만 켜두세요</div>
							<div style={{ borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, overflow:'hidden', maxWidth:560 }}>
								{[{k:'_lock',label:'신청 승인 / 반려 결과',desc:'관리자가 신청을 처리하면 알려드려요',lock:true},{k:'remind',label:'프로그램 시작 D-1 리마인더',desc:'시작 하루 전 잊지 않도록 알려드려요'},{k:'empty',label:'빈자리 알림',desc:'마감된 프로그램에 빈자리가 생기면 알려드려요'},{k:'news',label:'신규 프로그램 소식',desc:'새로운 프로그램이 열리면 알려드려요'}].map((e,i,arr)=>(
									<div key={e.k} style={{ display:'flex', alignItems:'center', gap:16, padding:'16px 18px', borderBottom:i<arr.length-1?`1px solid ${T.borderLight}`:'none' }}>
										<div style={{ flex:1 }}><div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:3 }}><span style={{ fontSize:14.5, fontWeight:600, color:T.text }}>{e.label}</span>{e.lock && <span style={{ padding:'1px 7px', borderRadius:T.tagR, background:T.borderLight, color:T.textTri, fontSize:10.5, fontWeight:600 }}>필수</span>}</div><div style={{ fontSize:12.5, color:T.textSec }}>{e.desc}</div></div>
										{e.lock ? <div style={{ opacity:0.5, pointerEvents:'none' }}><Toggle on={true}/></div> : <Toggle on={noti[e.k]} onClick={()=>setNoti(s=>({...s,[e.k]:!s[e.k]}))}/>}
									</div>
								))}
							</div>
							<div style={{ display:'flex', justifyContent:'center', marginTop:24, maxWidth:560 }}><Btn size="m" onClick={()=>addToast('알림 설정이 저장되었습니다.')}>저장</Btn></div>
						</div>
					)}
				</div>
			</div>
			{/* Cancel Modal — 공통 ConfirmDialog 사용 */}
			{showCancel && (
				<ConfirmDialog
					icon="close" variant="danger"
					title="신청 취소하시겠습니까?"
					message="취소 후에는 다시 신청하셔야 합니다."
					cancelText="돌아가기" confirmText="신청 취소"
					onClose={()=>{setShowCancel(false);setCancelError(false);setCancelReason('');setCancelEtc('');}}
					onConfirm={doCancel}
				>
					<span style={{ fontSize:14, fontWeight:600, color:T.text, display:'block', marginBottom:12 }}>취소 사유 선택 <span style={{color:T.error}}>*</span></span>
					{['단순 변심','일정이 맞지 않음','중복 신청','개인 사유','기타'].map(r=>(
						<label key={r} onClick={()=>{setCancelReason(r);setCancelError(false);}} style={{ display:'flex', alignItems:'center', gap:10, marginBottom:10, cursor:'pointer' }}>
							<div style={{ width:18, height:18, borderRadius:'50%', border:`2px solid ${cancelReason===r?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'border 150ms' }}>
								{cancelReason===r && <div style={{ width:9, height:9, borderRadius:'50%', background:T.primary }}/>}
							</div>
							<span style={{ fontSize:14, color:T.text }}>{r}</span>
						</label>
					))}
					{cancelReason==='기타' && (
						<textarea
							autoFocus
							value={cancelEtc}
							onChange={e=>{setCancelEtc(e.target.value);setCancelError(false);}}
							placeholder="취소 사유를 입력해 주세요. (최대 100자)"
							maxLength={100}
							style={{ width:'100%', minHeight:72, resize:'none', padding:'10px 12px', marginTop:2, marginBottom:10, borderRadius:8, border:`1px solid ${cancelError&&!cancelEtc.trim()?T.error:T.border}`, fontSize:14, fontFamily:'inherit', color:T.text, outline:'none', boxSizing:'border-box', lineHeight:1.5 }}
						/>
					)}
					{cancelError && <div style={{ fontSize:12, color:T.error, marginBottom:4 }}>{cancelReason==='기타'&&!cancelEtc.trim()?'취소 사유를 입력해 주세요.':'필수 선택 항목입니다.'}</div>}
				</ConfirmDialog>
			)}
			<Footer/>
		</div>
	);
}

// ── 신청 상세 (WF-3-001-03) ──
function ApplicationDetail({ go, pg, addToast }) {
	const [openA, setOpenA] = useState(true);
	const [openB, setOpenB] = useState(true);
	const app = pg || {};
	const status = app.appStatus || '승인';
	const appBadge = { '승인':{bg:T.successLight,c:T.success}, '대기':{bg:T.warningLight,c:T.warning}, '반려':{bg:'#FEF2F2',c:T.error}, '취소':{bg:T.borderLight,c:T.textTri} }; // 파스텔 통일안(확정 전) — 기획서 솔리드안: 승인=파랑·대기=검정·반려=빨강·취소=회색
	const b = appBadge[status]||appBadge['승인'];
	const canCancel = status==='승인' || status==='대기';
	const Section = ({ title, open, onToggle, children }) => (
		<div style={{ borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, padding:'18px 22px', marginBottom:16 }}>
			<div className="btn-hover" onClick={onToggle} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', cursor:'pointer' }}>
				<span style={{ fontSize:17, fontWeight:700, color:T.text }}>{title}</span>
				<Icon n={open?'chevU':'chevD'} size={20} color={T.textSec}/>
			</div>
			{open && <div style={{ marginTop:16 }}>{children}</div>}
		</div>
	);
	const Row = ({ k, v }) => (
		<div style={{ display:'flex', padding:'7px 0' }}>
			<span style={{ width:96, fontSize:14, color:T.textSec, flexShrink:0 }}>{k}</span>
			<span style={{ fontSize:14, color:T.text, flex:1 }}>{v}</span>
		</div>
	);
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
			<div style={{ display:'flex', gap:40, padding:'40px 80px' }}>
				{/* 사이드바 */}
				<div style={{ width:220, flexShrink:0 }}>
					<div style={{ borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, padding:'22px 20px', marginBottom:20 }}>
						<div style={{ fontWeight:700, fontSize:17, color:T.text, marginBottom:18 }}>반가워요! 박시현님</div>
						<div style={{ display:'flex', gap:18 }}>
							<div style={{ textAlign:'center', flex:1 }}><div style={{ fontSize:13, color:T.textSec, marginBottom:4 }}>진행중인 프로그램</div><div style={{ fontSize:22, fontWeight:700, color:T.text }}>2</div></div>
							<div style={{ width:1, background:T.border }}/>
							<div style={{ textAlign:'center', flex:1 }}><div style={{ fontSize:13, color:T.textSec, marginBottom:4 }}>종료된 프로그램</div><div style={{ fontSize:22, fontWeight:700, color:T.text }}>5</div></div>
						</div>
					</div>
					<div style={{ borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, padding:'12px 8px', display:'flex', flexDirection:'column', gap:2 }}>
						{[{icon:'calendar',label:'프로그램 신청 내역',tab:'history',on:true},{icon:'star',label:'즐겨찾기한 프로그램',tab:'favorites'},{icon:'bell',label:'알림 설정',tab:'noti'},{icon:'user',label:'개인 정보 수정',tab:'profile'}].map(m=>(
							<div key={m.tab} className="btn-hover" onClick={()=>go('mypage',{tab:m.tab})} style={{ display:'flex', alignItems:'center', gap:10, padding:'11px 12px', borderRadius:8, background:m.on?T.primaryBg:'transparent', cursor:'pointer' }}>
								<Icon n={m.icon} size={18} color={m.on?T.primary:T.textSec}/>
								<span style={{ fontSize:14, fontWeight:m.on?600:400, color:m.on?T.primary:T.textSec }}>{m.label}</span>
							</div>
						))}
					</div>
				</div>
				{/* 본문 */}
				<div style={{ flex:1, maxWidth:760 }}>
					<div className="btn-hover" onClick={()=>go('mypage',{tab:'history'})} style={{ display:'inline-flex', alignItems:'center', gap:6, marginBottom:18, cursor:'pointer', color:T.textSec, fontSize:14 }}>
						<Icon n="arrowL" size={18} color={T.textSec}/> 신청 내역으로
					</div>
					<h3 style={{ fontSize:20, fontWeight:700, color:T.text, marginBottom:10 }}>프로그램 신청 상세</h3>
					<div style={{ height:1, background:T.border, marginBottom:20 }}/>
					{/* 프로그램 신청 상세 카드 */}
					<div style={{ display:'flex', gap:18, padding:'18px 22px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, marginBottom:16 }}>
						<div style={{ width:96, height:96, borderRadius:10, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[(app.id||1)-1]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
						<div style={{ flex:1 }}>
							<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:8 }}>
								<span style={{ fontSize:17, fontWeight:700, color:T.text }}>{app.title||'프로그램 A'}</span>
								<span style={{ padding:'2px 10px', borderRadius:T.tagR, background:b.bg, color:b.c, fontSize:12, fontWeight:600 }}>{status}</span>
							</div>
							<div style={{ fontSize:14, color:T.textSec }}>{app.date||'2024-07-08 (월) 15:00'}</div>
						</div>
					</div>
					{/* 신청자 정보 */}
					<Section title="신청자 정보" open={openA} onToggle={()=>setOpenA(o=>!o)}>
						<Row k="이름" v="박시현"/>
						<Row k="핸드폰 번호" v="010-1234-5678"/>
						<Row k="성별" v="여"/>
						<Row k="생년월일" v="1998-03-21"/>
						<Row k="주소" v="(16922) 경기도 용인시 기흥구 용구대로 2311 (마북동)"/>
					</Section>
					{/* 신청 이력 */}
					<Section title="신청 이력" open={openB} onToggle={()=>setOpenB(o=>!o)}>
						<Row k="신청일시" v={app.appliedAt||'2024-07-05 17:11:31'}/>
						<Row k="승인일시" v={status==='승인'?'2024-07-06 09:05:31':'-'}/>
						<div style={{ display:'flex', padding:'7px 0' }}>
							<span style={{ width:96, fontSize:14, color:T.textSec, flexShrink:0 }}>담당자 의견</span>
							<span style={{ fontSize:14, color:T.text, flex:1, lineHeight:1.6 }}>프로그램 취소 신청은 프로그램 시작일의 최소 2일 전까지 부탁드립니다. 프로그램 당일 취소·노쇼 2회 이상 시 프로그램 참여가 제한됩니다.</span>
						</div>
					</Section>
					{/* 하단 버튼 */}
					{canCancel
						? <div className="btn-hover" onClick={()=>addToast('신청 취소가 접수되었습니다.')} style={{ height:52, borderRadius:T.tagR, background:T.text, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', marginTop:4 }}>
							<span style={{ fontSize:15, fontWeight:600, color:'#fff' }}>프로그램 신청 취소</span>
						</div>
						: <div className="btn-hover" onClick={()=>go('program-detail',{pg:app})} style={{ height:52, borderRadius:T.tagR, border:`1px solid ${T.text}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', marginTop:4 }}>
							<span style={{ fontSize:15, fontWeight:600, color:T.text }}>재신청</span>
						</div>}
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── WELCOME (회원가입 직후 온보딩 + 관심설정) ──
const WELCOME_REGIONS = ['수원시','성남시','고양시','용인시','부천시','안양시','안산시','화성시','남양주시','평택시','의정부시','시흥시'];
const WELCOME_CATS = ['취업·역량','창업','심리·건강','문화·예술','주거','금융','네트워킹'];
function WelcomeScreen({ go, addToast }) {
	const [regions, setRegions] = useState(new Set(['부천시']));
	const [cats, setCats] = useState(new Set(['취업·역량']));
	const toggle = (setter) => (v) => setter(s=>{ const n=new Set(s); n.has(v)?n.delete(v):n.add(v); return n; });
	const finish = (personalized) => {
		addToast(personalized ? '관심 정보가 저장되었어요. 맞춤 추천을 받아보세요!' : '청년모아에 오신 걸 환영해요!');
		go('home');
	};
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', alignItems:'flex-start', justifyContent:'center', padding:'56px 80px' }}>
				<div style={{ width:560 }}>
					{/* 환영 헤더 */}
					<div style={{ textAlign:'center', marginBottom:36 }}>
						<div style={{ width:72, height:72, borderRadius:'50%', background:T.primary, display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 20px', boxShadow:`0 8px 24px ${T.primary}40` }}>
							<svg viewBox="0 0 24 24" style={{ width:38, height:38 }}><path d="M5 12l4 4 10-10" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
						</div>
						<div style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:8 }}>환영합니다, 박시현님! 🎉</div>
						<div style={{ fontSize:15, color:T.textSec, lineHeight:1.6 }}>청년모아의 멤버가 되셨어요.<br/>관심 정보를 알려주시면 딱 맞는 프로그램을 추천해드릴게요.</div>
					</div>

					{/* 관심 지역 */}
					<div style={{ marginBottom:28 }}>
						<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:12 }}>
							<Icon n="pin" size={18} color={T.primary}/>
							<span style={{ fontSize:16, fontWeight:700, color:T.text }}>관심 지역</span>
							<span style={{ fontSize:13, color:T.textTri }}>중복 선택 가능</span>
						</div>
						<div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
							{WELCOME_REGIONS.map(r=>{ const on=regions.has(r); return (
								<div key={r} className="btn-hover" onClick={()=>toggle(setRegions)(r)} style={{ padding:'9px 16px', borderRadius:T.tagR, border:`1px solid ${on?T.primary:T.border}`, background:on?T.primaryBg:T.surface, cursor:'pointer', fontSize:14, fontWeight:on?600:400, color:on?T.primary:T.textSec }}>{r}</div>
							);})}
						</div>
					</div>

					{/* 관심 분야 */}
					<div style={{ marginBottom:36 }}>
						<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:12 }}>
							<Icon n="star" size={18} color={T.primary}/>
							<span style={{ fontSize:16, fontWeight:700, color:T.text }}>관심 분야</span>
							<span style={{ fontSize:13, color:T.textTri }}>중복 선택 가능</span>
						</div>
						<div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
							{WELCOME_CATS.map(cat=>{ const on=cats.has(cat); return (
								<div key={cat} className="btn-hover" onClick={()=>toggle(setCats)(cat)} style={{ padding:'9px 16px', borderRadius:T.tagR, border:`1px solid ${on?T.primary:T.border}`, background:on?T.primaryBg:T.surface, cursor:'pointer', fontSize:14, fontWeight:on?600:400, color:on?T.primary:T.textSec }}>{cat}</div>
							);})}
						</div>
					</div>

					{/* CTA */}
					<Btn size="l" fullWidth onClick={()=>finish(true)} style={{ marginBottom:10 }}>관심 정보 저장하고 시작하기</Btn>
					<div style={{ textAlign:'center' }}>
						<span className="btn-hover" onClick={()=>finish(false)} style={{ fontSize:14, color:T.textTri, cursor:'pointer', padding:'8px 12px', display:'inline-block' }}>나중에 할게요 · 건너뛰기</span>
					</div>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── SIGNUP (회원가입 폼 — Figma WF-2-001-01 기준) ──
function SignupScreen({ go, onLogin }) {
	const [form, setForm] = useState({ id:'', pw:'', pw2:'', name:'', phone:'', dob:'', zip:'', addr:'', addr2:'' });
	const [gender, setGender] = useState('');
	const [agree, setAgree] = useState({ terms:false, privacy:false });
	const set = (k)=>(e)=>setForm(f=>({...f,[k]:e.target.value}));
	const allAgreed = agree.terms && agree.privacy;
	const field = (on)=>({ height:46, borderRadius:8, border:`1.5px solid ${on?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms', boxSizing:'border-box', width:'100%', display:'block' });
	const fieldRow = (on)=>({ ...field(on), flex:1, width:'auto', minWidth:0 });
	const label = { fontSize:14, fontWeight:600, color:T.text, marginBottom:7, display:'block' };
	const sectionHead = { display:'flex', alignItems:'center', gap:8, paddingBottom:12, borderBottom:`2px solid ${T.text}`, marginBottom:18 };
	const submit = () => {
		if(!form.id||!form.pw||!form.pw2||!form.name||!form.phone||!gender||!form.dob){ alert('필수 항목을 모두 입력해주세요.'); return; }
		if(form.pw!==form.pw2){ alert('비밀번호가 일치하지 않습니다.'); return; }
		if(!allAgreed){ alert('필수 약관에 동의해주세요.'); return; }
		onLogin();
		go('welcome');
	};
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', justifyContent:'center', padding:'48px 80px' }}>
				<div style={{ width:520 }}>
					<div style={{ textAlign:'center', marginBottom:32 }}>
						<img src="assets/logo.png" alt="청년모아" style={{ height:34, margin:'0 auto 20px' }}/>
						<h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:8 }}>회원가입</h2>
						<p style={{ fontSize:14, color:T.textSec }}>청년모아 서비스 이용을 위해 회원정보를 입력해주세요.</p>
					</div>

					{/* 계정 정보 */}
					<div style={{ marginBottom:28 }}>
						<div style={sectionHead}><span style={{ fontWeight:700, fontSize:16, color:T.text }}>계정 정보</span></div>
						<div style={{ display:'flex', flexDirection:'column', gap:18 }}>
							<div>
								<span style={label}>아이디 <span style={{color:T.error}}>*</span></span>
								<div style={{ display:'flex', gap:8 }}>
									<input value={form.id} onChange={set('id')} placeholder="아이디를 입력해주세요" style={fieldRow(form.id)}/>
									<Btn size="m" variant="outline" onClick={()=>alert('사용 가능한 아이디입니다.')} style={{ flexShrink:0, width:96, height:46 }}>중복확인</Btn>
								</div>
							</div>
							<div>
								<span style={label}>비밀번호 <span style={{color:T.error}}>*</span></span>
								<input type="password" value={form.pw} onChange={set('pw')} placeholder="영문, 숫자 포함 8자 이상" style={field(form.pw)}/>
							</div>
							<div>
								<span style={label}>비밀번호 확인 <span style={{color:T.error}}>*</span></span>
								<input type="password" value={form.pw2} onChange={set('pw2')} placeholder="비밀번호를 다시 입력해주세요" style={field(form.pw2)}/>
								{form.pw2 && form.pw!==form.pw2 && <span style={{ fontSize:12, color:T.error, marginTop:5, display:'block' }}>비밀번호가 일치하지 않습니다.</span>}
							</div>
						</div>
					</div>

					{/* 개인 정보 */}
					<div style={{ marginBottom:28 }}>
						<div style={sectionHead}><span style={{ fontWeight:700, fontSize:16, color:T.text }}>개인 정보</span></div>
						<div style={{ display:'flex', flexDirection:'column', gap:18 }}>
							<div>
								<span style={label}>이름 <span style={{color:T.error}}>*</span></span>
								<input value={form.name} onChange={set('name')} placeholder="이름을 입력해주세요" style={field(form.name)}/>
							</div>
							<div>
								<span style={label}>핸드폰 번호 <span style={{color:T.error}}>*</span></span>
								<div style={{ display:'flex', gap:8 }}>
									<input value={form.phone} onChange={set('phone')} placeholder="숫자만 입력해주세요" style={fieldRow(form.phone)}/>
									<Btn size="m" variant="outline" onClick={()=>alert('인증번호를 발송했습니다.')} style={{ flexShrink:0, width:96, height:46 }}>인증요청</Btn>
								</div>
							</div>
							<div>
								<span style={label}>성별 <span style={{color:T.error}}>*</span></span>
								<div style={{ display:'flex', gap:20, paddingTop:4 }}>
									{['남','여'].map(g=>(
										<label key={g} onClick={()=>setGender(g)} style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer' }}>
											<div style={{ width:20, height:20, borderRadius:'50%', border:`2px solid ${gender===g?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center' }}>
												{gender===g && <div style={{ width:10, height:10, borderRadius:'50%', background:T.primary }}/>}
											</div>
											<span style={{ fontSize:14, color:T.text }}>{g}</span>
										</label>
									))}
								</div>
							</div>
							<div>
								<span style={label}>생년월일 <span style={{color:T.error}}>*</span></span>
								<input value={form.dob} onChange={set('dob')} placeholder="YYYY / MM / DD" style={field(form.dob)}/>
							</div>
							<div>
								<span style={label}>주소 <span style={{color:T.error}}>*</span></span>
								<div style={{ display:'flex', flexDirection:'column', gap:8 }}>
									<div style={{ display:'flex', gap:8 }}>
										<input value={form.zip} readOnly placeholder="우편번호" style={{ ...fieldRow(form.zip), background:'#F3F4F6', cursor:'default' }}/>
										<Btn size="m" variant="outline" icon="search" onClick={()=>setForm(f=>({...f, zip:'14547', addr:'경기도 부천시 원미구 부일로 123'}))} style={{ flexShrink:0, width:96, height:46 }}>검색</Btn>
									</div>
									<input value={form.addr} readOnly placeholder="주소 (검색으로 입력)" style={{ ...field(form.addr), background:'#F3F4F6', cursor:'default' }}/>
									<input value={form.addr2} onChange={set('addr2')} placeholder="상세주소를 입력해주세요" style={field(form.addr2)}/>
								</div>
							</div>
						</div>
					</div>

					{/* 이용약관 동의 */}
					<div style={{ marginBottom:28 }}>
						<div style={sectionHead}><span style={{ fontWeight:700, fontSize:16, color:T.text }}>이용약관 동의</span></div>
						<label onClick={()=>{ const v=!allAgreed; setAgree({terms:v,privacy:v}); }} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderBottom:`1px solid ${T.borderLight}`, cursor:'pointer', marginBottom:4 }}>
							<div style={{ width:22, height:22, borderRadius:6, background:allAgreed?T.primary:T.surface, border:`1.5px solid ${allAgreed?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
								{allAgreed && <svg viewBox="0 0 24 24" style={{width:14,height:14}}><path d="M5 12l4 4 10-10" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
							</div>
							<span style={{ fontSize:15, fontWeight:600, color:T.text }}>전체 동의</span>
						</label>
						{[{k:'terms',label:'회원가입약관'},{k:'privacy',label:'개인정보처리방침 안내'}].map(a=>(
							<div key={a.k} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'11px 0' }}>
								<label onClick={()=>setAgree(s=>({...s,[a.k]:!s[a.k]}))} style={{ display:'flex', alignItems:'center', gap:10, cursor:'pointer' }}>
									<svg viewBox="0 0 24 24" style={{width:18,height:18,flexShrink:0}}><path d="M5 12l4 4 10-10" stroke={agree[a.k]?T.primary:T.border} strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
									<span style={{ fontSize:14, color:T.textSec }}>{a.label} <span style={{color:T.error}}>(필수)</span></span>
								</label>
								<span className="btn-hover" onClick={()=>go('terms')} style={{ fontSize:12.5, color:T.primary, fontWeight:500, cursor:'pointer' }}>약관보기 ›</span>
							</div>
						))}
					</div>

					<div style={{ display:'flex', justifyContent:'center', gap:12 }}>
						<Btn size="l" variant="ghost" onClick={()=>go('login')} style={{ width:150 }}>취소</Btn>
						<Btn size="l" variant="primary" onClick={submit} style={{ width:200 }}>회원가입</Btn>
					</div>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── LOGIN ──
function LoginScreen({ go, onLogin }) {
	const [id, setId] = useState('');
	const [pw, setPw] = useState('');
	const handle = () => {
		if(!id||!pw){ alert('아이디와 비밀번호를 입력해주세요.'); return; }
		onLogin();
		go('home');
	};
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center' }}>
				<div style={{ width:400, textAlign:'center' }}>
					<img src="assets/logo.png" alt="청년모아" style={{ height:36, margin:'0 auto 28px' }}/>
					<h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:24 }}>로그인</h2>
					<div style={{ display:'flex', flexDirection:'column', gap:10, marginBottom:10 }}>
						<input value={id} onChange={e=>setId(e.target.value)} placeholder="아이디를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${id?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms' }}/>
						<input type="password" value={pw} onChange={e=>setPw(e.target.value)} onKeyDown={e=>e.key==='Enter'&&handle()} placeholder="비밀번호를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${pw?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms' }}/>
					</div>
					<div style={{ display:'flex', justifyContent:'space-between', marginBottom:20, fontSize:13 }}>
						<label style={{ display:'flex', alignItems:'center', gap:6, cursor:'pointer', color:T.textSec }}>
							<input type="checkbox" style={{ accentColor:T.primary }}/> 아이디 저장
						</label>
						<div style={{ display:'flex', gap:8, color:T.textSec }}>
							<span style={{ cursor:'pointer' }} onClick={()=>go('find-id')}>아이디 찾기</span>
							<span>|</span>
							<span style={{ cursor:'pointer' }} onClick={()=>go('find-id')}>비밀번호 찾기</span>
						</div>
					</div>
					<div style={{ display:'flex', flexDirection:'column', gap:10 }}>
						<Btn size="l" fullWidth onClick={handle}>로그인</Btn>
						<Btn size="l" variant="secondary" fullWidth onClick={()=>go('signup')}>회원가입</Btn>
					</div>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── NOTICES ──
function NoticesScreen({ go }) {
	const catColors = { '행사':{bg:T.secondaryLight,fg:T.secondary},'공지':{bg:T.primaryLight,fg:T.primary},'운영':{bg:T.successLight,fg:T.success},'기타':{bg:T.borderLight,fg:T.textTri} };
	const [cat, setCat] = useState('전체');
	const [page, setPage] = useState(1);
	const cats = ['전체','행사','공지','운영','기타'];
	const filtered = cat==='전체' ? NOTICES : NOTICES.filter(n=>n.cat===cat);
	// 고정글은 항상 상단, 나머지는 필터 적용
	const pinned = filtered.filter(n=>n.pin);
	const rest = filtered.filter(n=>!n.pin);
	const rows = [...pinned, ...rest];
	return (
		<div className="screen-enter" style={{ background:T.surface, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ padding:'28px 80px 0' }}>
				<h2 style={{ fontSize:28, fontWeight:700, color:T.text, textAlign:'center', marginBottom:20 }}>공지사항</h2>
			</div>
			<div style={{ padding:'0 80px 48px', flex:1 }}>
				{/* 카테고리 탭 (pill) */}
				<div style={{ display:'flex', justifyContent:'center', gap:8, marginBottom:20 }}>
					{cats.map(c=>(
						<div key={c} className="btn-hover" onClick={()=>{setCat(c);setPage(1);}} style={{ padding:'8px 18px', borderRadius:T.tagR, border:`1px solid ${cat===c?T.primary:T.border}`, background:cat===c?T.primary:T.surface, cursor:'pointer', fontSize:13.5, fontWeight:cat===c?600:400, color:cat===c?'#fff':T.textSec }}>{c}</div>
					))}
				</div>
				<div style={{ borderTop:`1px solid ${T.text}`, borderBottom:`1px solid ${T.border}` }}>
					<div style={{ display:'grid', gridTemplateColumns:'80px 80px 1fr 120px 80px', padding:'10px 16px', background:T.borderLight, borderBottom:`1px solid ${T.border}` }}>
						{['No','구분','제목','작성일','조회수'].map(h=><span key={h} style={{ fontSize:13, fontWeight:600, color:T.textSec, textAlign:h==='제목'?'left':'center' }}>{h}</span>)}
					</div>
					{rows.length===0 ? (
						<div style={{ padding:'60px 0', textAlign:'center', fontSize:14, color:T.textSec }}>해당 구분의 공지사항이 없습니다.</div>
					) : rows.map((n,i)=>(
						<div key={n.id} onClick={()=>go('notice-detail',{notice:n})} className="card-hover" style={{ display:'grid', gridTemplateColumns:'80px 80px 1fr 120px 80px', padding:'14px 16px', borderBottom:`1px solid ${T.borderLight}`, alignItems:'center', background:n.pin?T.primaryBg:'transparent', cursor:'pointer' }}>
							<span style={{ fontSize:13, color:T.textSec, textAlign:'center' }}>{n.id}</span>
							<div style={{ textAlign:'center' }}><span style={{ display:'inline-block', padding:'2px 10px', borderRadius:T.tagR, background:(catColors[n.cat]||{}).bg||T.borderLight, color:(catColors[n.cat]||{}).fg||T.textTri, fontSize:11, fontWeight:600 }}>{n.cat}</span></div>
							<div style={{ display:'flex', alignItems:'center', gap:6 }}>
								{n.pin && <span style={{ fontSize:11, color:T.primary }}>📌</span>}
								<span style={{ fontSize:14, color:T.text, fontWeight:n.pin?600:400 }}>{n.title}</span>
							</div>
							<span style={{ fontSize:13, color:T.textTri, textAlign:'center' }}>{n.date}</span>
							<span style={{ fontSize:13, color:T.textTri, textAlign:'center' }}>{n.views.toLocaleString()}</span>
						</div>
					))}
				</div>
				{/* 페이지네이션 */}
				{rows.length>0 && (
					<div style={{ display:'flex', justifyContent:'center', alignItems:'center', gap:4, marginTop:28 }}>
						<div className="btn-hover" onClick={()=>setPage(p=>Math.max(1,p-1))} style={{ width:32, height:32, borderRadius:7, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', background:T.surface, cursor:'pointer' }}>
							<span style={{ fontSize:13, color:T.textTri }}>‹</span>
						</div>
						{[1,2,3].map(n=>(
							<div key={n} className="btn-hover" onClick={()=>setPage(n)} style={{ width:32, height:32, borderRadius:7, border:page===n?'none':`1px solid ${T.border}`, background:page===n?T.primary:T.surface, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
								<span style={{ fontSize:13, fontWeight:page===n?700:500, color:page===n?'#fff':T.textSec, fontFamily:'Inter,sans-serif' }}>{n}</span>
							</div>
						))}
						<div className="btn-hover" onClick={()=>setPage(p=>Math.min(3,p+1))} style={{ width:32, height:32, borderRadius:7, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', background:T.surface, cursor:'pointer' }}>
							<span style={{ fontSize:13, color:T.textSec }}>›</span>
						</div>
					</div>
				)}
			</div>
			<Footer/>
		</div>
	);
}

// ── NOTICE DETAIL (§5.14) ──
function NoticeDetail({ go, notice }) {
	const n = notice || NOTICES[0];
	const catColors = { '행사':{bg:T.secondaryLight,fg:T.secondary},'공지':{bg:T.primaryLight,fg:T.primary},'운영':{bg:T.successLight,fg:T.success},'기타':{bg:T.borderLight,fg:T.textTri} };
	const cc = catColors[n.cat]||{bg:T.borderLight,fg:T.textTri};
	return (
		<div className="screen-enter" style={{ background:T.surface, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ maxWidth:900, width:'100%', margin:'0 auto', padding:'32px 24px 56px', flex:1, boxSizing:'border-box' }}>
				<div className="btn-hover" onClick={()=>go('notices')} title="목록으로" style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:38, height:38, borderRadius:9, border:`1px solid ${T.border}`, background:T.surface, marginBottom:20, cursor:'pointer' }}>
					<Icon n="arrowL" size={18} color={T.textSec}/>
				</div>
				<div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:12 }}>
					<span style={{ display:'inline-block', padding:'3px 12px', borderRadius:T.tagR, background:cc.bg, color:cc.fg, fontSize:12, fontWeight:600 }}>{n.cat}</span>
					{n.pin && <span style={{ fontSize:12, color:T.primary, fontWeight:600 }}>📌 고정</span>}
				</div>
				<h1 style={{ fontSize:26, fontWeight:700, color:T.text, margin:'0 0 12px', lineHeight:1.35 }}>{n.title}</h1>
				<div style={{ display:'flex', gap:16, fontSize:13, color:T.textTri, paddingBottom:20, borderBottom:`1px solid ${T.border}` }}>
					<span>작성일 {n.date}</span>
					<span>조회 {n.views.toLocaleString()}</span>
				</div>
				{/* 본문 */}
				<div style={{ padding:'28px 0', fontSize:15, color:T.text, lineHeight:1.85 }}>
					<p style={{ margin:'0 0 18px' }}>안녕하세요, 경기도 청년모아입니다.</p>
					<p style={{ margin:'0 0 18px' }}>「{n.title}」 관련하여 아래와 같이 안내드립니다. 자세한 내용은 첨부파일을 확인해 주시기 바랍니다.</p>
					<div style={{ width:'100%', height:280, borderRadius:T.radius, overflow:'hidden', background:'#e5e7eb', margin:'8px 0 20px' }}>
						<img src="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=860&h=280&fit=crop" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
					</div>
					<p style={{ margin:'0 0 8px' }}>· 대상: 경기도 거주 만 19~39세 청년</p>
					<p style={{ margin:'0 0 8px' }}>· 신청: 청년모아 홈페이지 및 방문 접수</p>
					<p style={{ margin:'0 0 18px' }}>· 문의: 청년센터 대표번호 031-000-0000</p>
					<p style={{ margin:0 }}>많은 관심과 참여 부탁드립니다. 감사합니다.</p>
				</div>
				{/* 첨부파일 */}
				<div style={{ display:'flex', alignItems:'center', gap:10, padding:'14px 16px', borderRadius:T.radius, background:T.bg, border:`1px solid ${T.borderLight}`, marginBottom:24 }}>
					<Icon n="download" size={18} color={T.primary}/>
					<span style={{ flex:1, fontSize:14, color:T.text }}>{n.title}_안내문.pdf</span>
					<span style={{ fontSize:12, color:T.textTri }}>1.2MB</span>
				</div>
				{/* 이전/다음 + 목록 */}
				<div style={{ borderTop:`1px solid ${T.border}` }}>
					{[['이전글','7월 청년센터 프로그램 일정 안내'],['다음글','7월 휴관 일정 안내']].map(([k,t])=>(
						<div key={k} className="card-hover" onClick={()=>go('notices')} style={{ display:'flex', alignItems:'center', gap:16, padding:'14px 8px', borderBottom:`1px solid ${T.borderLight}`, cursor:'pointer' }}>
							<span style={{ fontSize:13, fontWeight:600, color:T.textSec, width:48, flexShrink:0 }}>{k}</span>
							<span style={{ fontSize:14, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{t}</span>
						</div>
					))}
				</div>
				<div style={{ display:'flex', justifyContent:'center', marginTop:28 }}>
					<Btn variant="secondary" size="m" onClick={()=>go('notices')}>목록으로</Btn>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── CENTER DATA ──
const CENTER_DATA = [
	{ id:1, name:'상상대로', region:'수원시', addr:'경기도 수원시 팔달구 매산로 89', hours:'평일 09:00~18:00', tel:'031-228-1234', programs:7, x:42, y:58, open:true, desc:'청년 창업과 네트워킹을 위한 복합문화공간' },
	{ id:2, name:'내일스퀘어', region:'부천시', addr:'경기도 부천시 원미구 길주로 210', hours:'평일 09:00~18:00', tel:'032-320-5678', programs:11, x:28, y:44, open:true, desc:'취업·역량강화 특화 청년지원센터' },
	{ id:3, name:'비행지구', region:'고양시', addr:'경기도 고양시 일산동구 중앙로 1275', hours:'평일 10:00~19:00', tel:'031-908-4321', programs:3, x:30, y:24, open:true, desc:'문화·예술 기반 청년활동 지원공간' },
	{ id:4, name:'범계역 청년출구', region:'안양시', addr:'경기도 안양시 동안구 시민대로 180', hours:'평일 09:00~18:00', tel:'031-389-7788', programs:5, x:40, y:50, open:false, desc:'정신건강·힐링 프로그램 전문 센터' },
	{ id:5, name:'오름', region:'용인시', addr:'경기도 용인시 기흥구 용구대로 2311', hours:'평일 09:00~18:00', tel:'031-324-9900', programs:4, x:56, y:64, open:true, desc:'소셜벤처·사회적 경제 청년 지원' },
	{ id:6, name:'고천센터', region:'의왕시', addr:'경기도 의왕시 고천로 50', hours:'평일 09:00~18:00', tel:'031-345-2200', programs:2, x:44, y:62, open:true, desc:'지역사회 연계 청년 커뮤니티 허브' },
	{ id:7, name:'딴딴회관', region:'군포시', addr:'경기도 군포시 산본로 100', hours:'평일 10:00~18:00', tel:'031-456-3300', programs:1, x:38, y:60, open:true, desc:'주거·생활 밀착형 청년지원 거점' },
	{ id:8, name:'이루잡', region:'화성시', addr:'경기도 화성시 동탄대로 645', hours:'평일 09:00~18:00', tel:'031-567-4400', programs:6, x:48, y:74, open:true, desc:'취업·진로 전문 지원 청년센터' },
];

const ALL_REGIONS_C = ['수원시','성남시','부천시','안양시','고양시','용인시','의왕시','군포시','화성시','평택시','시흥시','광명시','남양주시','파주시','김포시','하남시','오산시','광주시','이천시','양주시','구리시','의정부시','동두천시','포천시','여주시','가평군','양평군','연천군'];

// ── CENTERS SCREEN ──
function CentersScreen({ go, addToast }) {
	const [selectedId, setSelectedId] = useState(null);
	const [detailId, setDetailId] = useState(null);
	const [regionOpen, setRegionOpen] = useState(false);
	const [regionQuery, setRegionQuery] = useState('');
	const [selectedRegion, setSelectedRegion] = useState(null);
	const [onlyOpen, setOnlyOpen] = useState(false);
	const [centerSearch, setCenterSearch] = useState('');
	const [sortBy, setSortBy] = useState('name'); // name | programs

	const filtered = CENTER_DATA.filter(c =>
		(!selectedRegion || c.region === selectedRegion) &&
		(!onlyOpen || c.open) &&
		(!centerSearch || c.name.includes(centerSearch) || c.region.includes(centerSearch))
	).sort((a,b)=> sortBy==='programs' ? b.programs-a.programs : a.name.localeCompare(b.name,'ko'));
	const filteredRegions = ALL_REGIONS_C.filter(r => r.includes(regionQuery));
	const detailCenter = CENTER_DATA.find(c => c.id === detailId);
	const infoCenter = CENTER_DATA.find(c => c.id === selectedId);

	const handleMarker = (id) => {
		setSelectedId(prev => prev === id ? null : id);
		setDetailId(null);
	};
	const handleCard = (id) => {
		setDetailId(prev => prev === id ? null : id);
		setSelectedId(id);
	};
	const selectRegion = (r) => {
		setSelectedRegion(r);
		setRegionOpen(false);
		setRegionQuery('');
	};
	const clearRegion = (e) => { e.stopPropagation(); setSelectedRegion(null); };

	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			{/* Filter bar */}
			<div style={{ background:T.surface, borderBottom:`1px solid ${T.border}`, padding:'0 80px' }}>
				<div style={{ display:'flex', alignItems:'center', gap:12, height:56 }}>
					{/* 센터명 검색 */}
					<div style={{ width:250, height:38, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', padding:'0 12px', gap:8, background:T.surface }}>
						<Icon n="search" size={15} color={T.textTri}/>
						<input value={centerSearch} onChange={e=>setCenterSearch(e.target.value)} placeholder="센터명, 지역 검색" style={{ flex:1, border:'none', outline:'none', fontSize:13, fontFamily:'Pretendard', color:T.text, background:'transparent' }}/>
						{centerSearch && <Icon n="close" size={14} color={T.textTri} style={{cursor:'pointer'}} onClick={()=>setCenterSearch('')}/>}
					</div>
					{/* 지역 드롭다운 */}
					<div style={{ position:'relative' }}>
						<div onClick={()=>{setRegionOpen(v=>!v); setRegionQuery('');}}
							 style={{ height:38, minWidth:140, padding:'0 12px', borderRadius:8, border:`1.5px solid ${selectedRegion||regionOpen?T.primary:T.border}`, background:selectedRegion?T.primaryBg:T.surface, display:'flex', alignItems:'center', gap:8, cursor:'pointer', userSelect:'none' }}>
							{selectedRegion ? (
								<>
									<Icon n="pin" size={14} color={T.primary}/>
									<span style={{ fontSize:13, fontWeight:600, color:T.primary, flex:1 }}>{selectedRegion}</span>
									<div onClick={clearRegion} style={{ width:18, height:18, borderRadius:'50%', background:T.primary, display:'flex', alignItems:'center', justifyContent:'center' }}>
										<Icon n="close" size={10} color="#fff"/>
									</div>
								</>
							) : (
								<>
									<span style={{ fontSize:13, color:regionOpen?T.primary:T.textSec, flex:1 }}>지역 선택</span>
									<Icon n={regionOpen?'chevU':'chevD'} size={14} color={regionOpen?T.primary:T.textSec}/>
								</>
							)}
						</div>
						{regionOpen && (
							<div style={{ position:'absolute', top:44, left:0, width:260, background:T.surface, borderRadius:12, boxShadow:'0 8px 32px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, zIndex:300, overflow:'hidden' }}>
								<div style={{ padding:'10px 12px', borderBottom:`1px solid ${T.borderLight}` }}>
									<div style={{ display:'flex', alignItems:'center', gap:7, height:34, borderRadius:7, border:`1.5px solid ${T.primary}`, padding:'0 10px' }}>
										<Icon n="search" size={15} color={T.primary}/>
										<input autoFocus value={regionQuery} onChange={e=>setRegionQuery(e.target.value)} placeholder="시·군 이름 검색" style={{ flex:1, border:'none', outline:'none', fontSize:13, fontFamily:'Pretendard', color:T.text, background:'transparent' }}/>
										{regionQuery && <Icon n="close" size={13} color={T.textTri} style={{cursor:'pointer'}} onClick={()=>setRegionQuery('')}/>}
									</div>
								</div>
								<div style={{ maxHeight:220, overflowY:'auto', padding:'6px 0' }}>
									{filteredRegions.length===0
										? <div style={{ padding:'20px', textAlign:'center', fontSize:13, color:T.textTri }}>검색 결과가 없습니다</div>
										: filteredRegions.map((r,i) => {
											const on = r===selectedRegion;
											const idx = regionQuery ? r.indexOf(regionQuery) : -1;
											return (
												<div key={r+i} onClick={()=>selectRegion(r)} className="btn-hover"
													 style={{ display:'flex', alignItems:'center', gap:9, padding:'9px 14px', cursor:'pointer', background:on?T.primaryBg:'transparent' }}>
													<div style={{ width:17, height:17, borderRadius:4, border:`1.5px solid ${on?T.primary:T.border}`, background:on?T.primary:'#fff', flexShrink:0, display:'flex', alignItems:'center', justifyContent:'center' }}>
														{on && <svg viewBox="0 0 24 24" style={{width:10,height:10}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
													</div>
													<span style={{ fontSize:14, color:on?T.primary:T.text, fontWeight:on?600:400 }}>
                            {idx>=0
								? <>{r.slice(0,idx)}<span style={{background:'#FEF08A',fontWeight:700,borderRadius:2,padding:'0 1px'}}>{r.slice(idx,idx+regionQuery.length)}</span>{r.slice(idx+regionQuery.length)}</>
								: r}
                          </span>
												</div>
											);
										})}
								</div>
							</div>
						)}
					</div>
					<div style={{ flex:1 }}/>
					{/* 운영중 토글 */}
					<div style={{ display:'flex', alignItems:'center', gap:8 }}>
						<div onClick={()=>setOnlyOpen(v=>!v)} style={{ width:40, height:22, borderRadius:11, background:onlyOpen?T.primary:T.border, position:'relative', cursor:'pointer', transition:'background 200ms' }}>
							<div style={{ width:18, height:18, borderRadius:'50%', background:'#fff', position:'absolute', top:2, left:onlyOpen?20:2, transition:'left 200ms', boxShadow:'0 1px 3px rgba(0,0,0,0.2)' }}/>
						</div>
						<span style={{ fontSize:13, color:T.text }}>운영중만 보기</span>
					</div>
				</div>
			</div>
			{/* 콘텐츠 */}
			<div style={{ display:'flex', flex:1, padding:'16px 80px 40px', gap:16, alignItems:'flex-start' }} onClick={()=>{if(regionOpen)setRegionOpen(false);}}>
				{/* 리스트 */}
				<div style={{ width:detailId?240:360, flexShrink:0, transition:'width 250ms ease' }}>
					<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:10 }}>
						<div style={{ fontSize:13, color:T.textSec }}>총 <strong style={{color:T.text}}>{filtered.length}</strong>개 센터</div>
						{!detailId && (
							<div style={{ display:'flex', gap:4 }}>
								{[['name','이름순'],['programs','프로그램많은순']].map(([v,l])=>(
									<div key={v} className="btn-hover" onClick={()=>setSortBy(v)} style={{ padding:'4px 10px', borderRadius:T.tagR, fontSize:12, fontWeight:sortBy===v?600:400, color:sortBy===v?T.primary:T.textTri, background:sortBy===v?T.primaryBg:'transparent', cursor:'pointer' }}>{l}</div>
								))}
							</div>
						)}
					</div>
					<div className="scroll-container" style={{ display:'flex', flexDirection:'column', gap:8, maxHeight:600, overflowY:'auto', paddingRight:4 }}>
						{filtered.length===0
							? <div style={{ textAlign:'center', padding:'40px 0', color:T.textTri, fontSize:14 }}>조건에 맞는 센터가 없습니다</div>
							: filtered.map(c => (
								<div key={c.id} onClick={()=>handleCard(c.id)} className="card-hover"
									 style={{ padding:detailId?'10px 12px':'14px', borderRadius:T.radius, background:(c.id===detailId||c.id===selectedId)?T.primaryBg:T.surface, border:`1.5px solid ${(c.id===detailId||c.id===selectedId)?T.primary:T.borderLight}`, cursor:'pointer', transition:'all 200ms' }}>
									{detailId ? (
										<div style={{ display:'flex', alignItems:'center', gap:8 }}>
											<div style={{ width:30, height:30, borderRadius:6, background:c.id===detailId?T.primary:T.borderLight, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
												<Icon n="pin" size={15} color={c.id===detailId?'#fff':T.textTri}/>
											</div>
											<div style={{ flex:1, minWidth:0 }}>
												<div style={{ fontWeight:600, fontSize:13, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{c.name}</div>
												<div style={{ fontSize:11, color:T.textTri }}>{c.region}</div>
											</div>
											<span style={{ padding:'1px 6px', borderRadius:T.tagR, background:c.open?T.successLight:T.borderLight, color:c.open?T.success:T.textTri, fontSize:10, fontWeight:600, flexShrink:0 }}>{c.open?'운영중':'종료'}</span>
										</div>
									) : (
										<>
											<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:6 }}>
												<div style={{ display:'flex', alignItems:'center', gap:7 }}>
													<span style={{ fontWeight:700, fontSize:15, color:T.text }}>{c.name}</span>
													<span style={{ padding:'2px 7px', borderRadius:T.tagR, background:c.open?T.successLight:T.borderLight, color:c.open?T.success:T.textTri, fontSize:11, fontWeight:600 }}>{c.open?'운영중':'운영종료'}</span>
												</div>
											</div>
											<div style={{ fontSize:12, color:T.textSec, marginBottom:6, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{c.addr}</div>
											<div style={{ display:'flex', justifyContent:'space-between', paddingTop:8, borderTop:`1px solid ${T.borderLight}`, fontSize:12 }}>
												<span style={{ color:T.primary, fontWeight:600 }}>프로그램 {c.programs}건</span>
												<span style={{ color:T.textTri }}>상세보기 →</span>
											</div>
										</>
									)}
								</div>
							))}
					</div>
				</div>

				{/* 상세 패널 */}
				{detailCenter && (
					<div style={{ width:320, flexShrink:0, background:T.surface, borderRadius:T.radius, border:`1px solid ${T.border}`, overflow:'hidden', boxShadow:'0 4px 16px rgba(63,48,233,0.08)', transition:'all 300ms' }}>
						<div style={{ width:'100%', height:160, position:'relative', overflow:'hidden', background:'#E5E7EB' }}>
							<img src="https://images.unsplash.com/photo-1497366216548-37526070297c?w=400&h=200&fit=crop&auto=format" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
							<div style={{ position:'absolute', inset:0, background:'linear-gradient(transparent 50%, rgba(0,0,0,0.45))' }}/>
							<div onClick={()=>{setDetailId(null);setSelectedId(null);}} style={{ position:'absolute', top:10, right:10, width:28, height:28, borderRadius:'50%', background:'rgba(0,0,0,0.35)', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
								<Icon n="close" size={14} color="#fff"/>
							</div>
							<span style={{ position:'absolute', bottom:10, left:12, padding:'3px 10px', borderRadius:T.tagR, background:detailCenter.open?T.success:'#9CA3AF', color:'#fff', fontSize:11, fontWeight:600 }}>{detailCenter.open?'운영중':'운영종료'}</span>
						</div>
						<div style={{ padding:'16px 18px' }}>
							<div style={{ fontWeight:700, fontSize:17, color:T.text, marginBottom:4 }}>{detailCenter.name}</div>
							<div style={{ fontSize:13, color:T.textSec, marginBottom:12, lineHeight:1.6 }}>{detailCenter.desc}</div>
							<div style={{ display:'flex', flexDirection:'column', gap:8, marginBottom:14 }}>
								{[['pin',detailCenter.addr],['calendar',detailCenter.hours],['user',detailCenter.tel]].map(([icon,val],i)=>(
									<div key={i} style={{ display:'flex', alignItems:'flex-start', gap:8 }}>
										<div style={{ width:26, height:26, borderRadius:6, background:T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
											<Icon n={icon} size={13} color={T.primary}/>
										</div>
										<span style={{ fontSize:12, color:T.textSec, lineHeight:1.5, paddingTop:4 }}>{val}</span>
									</div>
								))}
							</div>
							<div style={{ background:T.bg, borderRadius:8, padding:'10px 12px', marginBottom:14 }}>
								<div style={{ fontSize:12, color:T.textSec, marginBottom:6 }}>진행중인 프로그램</div>
								<div style={{ fontSize:20, fontWeight:700, color:T.primary }}>{detailCenter.programs}건</div>
							</div>
							<Btn size="m" variant="primary" fullWidth onClick={()=>go('programs')}>프로그램 전체보기</Btn>
						</div>
					</div>
				)}

				{/* 지도 */}
				<div style={{ flex:1, borderRadius:T.radius, overflow:'hidden', border:`1px solid ${T.border}`, position:'relative', height:640, minHeight:640 }}>
					{/* SVG Map */}
					<div style={{ width:'100%', height:'100%', background:'#E8ECF3', position:'relative' }}>
						<svg width="100%" height="100%" style={{ position:'absolute', inset:0 }} preserveAspectRatio="xMidYMid slice">
							<defs><pattern id="cgrid" width="60" height="60" patternUnits="userSpaceOnUse"><path d="M 60 0 L 0 0 0 60" fill="none" stroke="#D8DDE8" strokeWidth="0.8"/></pattern></defs>
							<rect width="100%" height="100%" fill="url(#cgrid)"/>
							<rect x="-5%" y="28%" width="110%" height="9" rx="4" fill="#fff" transform="rotate(-7 50% 28%)"/>
							<rect x="-5%" y="60%" width="110%" height="11" rx="5" fill="#fff" transform="rotate(4 50% 60%)"/>
							<rect x="46%" y="-5%" width="9" height="110%" rx="4" fill="#fff" transform="rotate(5 46% 50%)"/>
							<ellipse cx="85%" cy="15%" rx="8%" ry="6%" fill="#D1E8F5" opacity="0.8"/>
							<ellipse cx="12%" cy="72%" rx="9%" ry="6%" fill="#D4EDD9" opacity="0.8"/>
						</svg>
						{/* Markers */}
						{filtered.map(c => {
							const on = c.id===selectedId || c.id===detailId;
							return (
								<div key={c.id} onClick={()=>handleMarker(c.id)}
									 style={{ position:'absolute', left:`${c.x}%`, top:`${c.y}%`, transform:'translate(-50%,-100%)', zIndex:on?20:10, cursor:'pointer' }}>
									<div style={{ display:'flex', flexDirection:'column', alignItems:'center', filter:on?'drop-shadow(0 4px 8px rgba(63,48,233,0.45))':'drop-shadow(0 2px 4px rgba(0,0,0,0.2))' }}>
										<div style={{ background:on?T.primary:(c.open?'#fff':'#E5E7EB'), border:`2.5px solid ${on?T.primary:(c.open?T.primary:T.border)}`, borderRadius:on?'14px':'50%', padding:on?'5px 12px':'0', width:on?'auto':30, height:30, display:'flex', alignItems:'center', justifyContent:'center', transition:'all 200ms ease', gap:5 }}>
											<Icon n="pin" size={15} color={on?'#fff':(c.open?T.primary:T.textTri)}/>
											{on && <span style={{ fontSize:12, fontWeight:700, color:'#fff', whiteSpace:'nowrap' }}>{c.name}</span>}
										</div>
										<div style={{ width:2, height:8, background:on?T.primary:(c.open?T.primary:T.border) }}/>
									</div>
								</div>
							);
						})}
						{/* 이 지역에서 검색 — 지도 이동 시 노출 (데모: 상시) */}
						<div style={{ position:'absolute', top:14, left:'50%', transform:'translateX(-50%)', zIndex:25 }}>
							<div className="btn-hover" onClick={()=>addToast('현재 지도 영역의 센터를 검색했어요.')} style={{ display:'flex', alignItems:'center', gap:6, padding:'8px 16px', borderRadius:T.tagR, background:T.surface, boxShadow:'0 3px 12px rgba(0,0,0,0.16)', cursor:'pointer' }}>
								<Icon n="refresh" size={14} color={T.primary}/>
								<span style={{ fontSize:13, fontWeight:600, color:T.primary }}>이 지역에서 검색</span>
							</div>
						</div>
						{/* 카카오맵 라벨 */}
						<div style={{ position:'absolute', bottom:12, right:14, background:'rgba(255,255,255,0.88)', borderRadius:6, padding:'4px 10px', backdropFilter:'blur(4px)' }}>
							<span style={{ fontFamily:'monospace', fontSize:11, color:'#999' }}>카카오맵 API 연동 예정</span>
						</div>
						{/* 인포윈도우 */}
						{infoCenter && !detailId && (
							<div style={{ position:'absolute', left:`min(calc(${infoCenter.x}% - 150px), calc(100% - 320px))`, top:`calc(${infoCenter.y}% - 240px)`, zIndex:30, width:300, background:T.surface, borderRadius:14, boxShadow:'0 8px 32px rgba(0,0,0,0.18)', overflow:'hidden' }}>
								<div style={{ width:'100%', height:110, position:'relative', overflow:'hidden', background:'#E5E7EB' }}>
									<img src="https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=360&h=140&fit=crop&auto=format" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
									<div style={{ position:'absolute', inset:0, background:'linear-gradient(transparent 40%, rgba(0,0,0,0.4))' }}/>
									<div onClick={()=>setSelectedId(null)} style={{ position:'absolute', top:8, right:8, width:24, height:24, borderRadius:'50%', background:'rgba(0,0,0,0.3)', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
										<Icon n="close" size={12} color="#fff"/>
									</div>
									<span style={{ position:'absolute', bottom:8, left:10, padding:'2px 8px', borderRadius:T.tagR, background:infoCenter.open?T.success:'#9CA3AF', color:'#fff', fontSize:10, fontWeight:600 }}>{infoCenter.open?'운영중':'운영종료'}</span>
								</div>
								<div style={{ padding:'12px 14px' }}>
									<div style={{ fontWeight:700, fontSize:15, color:T.text, marginBottom:3 }}>{infoCenter.name}</div>
									<div style={{ fontSize:12, color:T.textSec, marginBottom:8, lineHeight:1.5 }}>{infoCenter.addr}</div>
									<div style={{ fontSize:11, color:T.textTri, marginBottom:10 }}>🕒 {infoCenter.hours}</div>
									<div style={{ display:'flex', gap:8 }}>
										<div onClick={()=>{setDetailId(infoCenter.id);setSelectedId(null);}} className="btn-hover"
											 style={{ flex:1, height:32, background:T.primary, borderRadius:7, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', gap:4 }}>
											<span style={{ fontSize:12, fontWeight:600, color:'#fff' }}>상세보기</span>
										</div>
										<div className="btn-hover" style={{ width:32, height:32, border:`1px solid ${T.border}`, borderRadius:7, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
											<Icon n="share" size={14} color={T.textSec}/>
										</div>
									</div>
								</div>
							</div>
						)}
					</div>
				</div>
			</div>
			<Footer/>
		</div>
	);
}

// ── SEARCH ──
const POPULAR_KEYWORDS = ['취업', '창업', '면접', '자기소개서', '심리상담', '주거지원', 'AI'];
function SearchScreen({ go, initialQ }) {
	const [q, setQ] = useState(initialQ||'');
	const [recent, setRecent] = useState(['디지털 마케팅', '창업 아카데미']);
	const inputRef = useRef(null);
	useEffect(()=>{ inputRef.current && inputRef.current.focus(); }, []);
	const ql = q.trim().toLowerCase();
	const suggestions = ql ? PROGRAMS.filter(p=> p.title.toLowerCase().includes(ql) || p.center.toLowerCase().includes(ql) || p.region.includes(q.trim())).slice(0,6) : [];
	const submit = (kw) => { const k=(kw??q).trim(); if(!k) return; setRecent(r=>[k, ...r.filter(x=>x!==k)].slice(0,6)); };
	const hi = (text) => {
		if(!ql) return text;
		const idx = text.toLowerCase().indexOf(ql);
		if(idx<0) return text;
		return <>{text.slice(0,idx)}<span style={{ color:T.primary, fontWeight:700 }}>{text.slice(idx,idx+ql.length)}</span>{text.slice(idx+ql.length)}</>;
	};
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ background:T.surface, borderBottom:`1px solid ${T.borderLight}`, padding:'24px 80px' }}>
				<div style={{ maxWidth:720, margin:'0 auto', display:'flex', alignItems:'center', gap:12, height:54, border:`2px solid ${T.primary}`, borderRadius:T.tagR, padding:'0 8px 0 20px', background:T.surface }}>
					<Icon n="search" size={20} color={T.primary}/>
					<input ref={inputRef} value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submit()} aria-label="프로그램 검색" placeholder="프로그램, 센터명, 지역으로 검색" style={{ flex:1, border:'none', outline:'none', fontSize:16, color:T.text, background:'transparent', fontFamily:'Pretendard' }}/>
					{q && <div className="btn-hover" onClick={()=>setQ('')} style={{ cursor:'pointer', padding:4 }}><Icon n="close" size={18} color={T.textTri}/></div>}
					<div className="btn-hover" onClick={()=>submit()} style={{ height:38, padding:'0 20px', background:T.primary, borderRadius:T.tagR, display:'flex', alignItems:'center', cursor:'pointer' }}><span style={{ color:'#fff', fontSize:14, fontWeight:600 }}>검색</span></div>
				</div>
			</div>
			<div style={{ maxWidth:720, margin:'0 auto', width:'100%', padding:'28px 80px 48px', flex:1 }}>
				{ql ? (
					<div>
						<div style={{ fontSize:13, color:T.textSec, marginBottom:12 }}>추천 검색</div>
						{suggestions.length===0
							? <div style={{ padding:'48px 0', textAlign:'center', color:T.textTri, fontSize:14 }}>'{q}'에 대한 결과가 없어요</div>
							: <div style={{ display:'flex', flexDirection:'column', gap:2 }}>
								{suggestions.map(pg=>(
									<div key={pg.id} className="btn-hover" onClick={()=>{submit(pg.title);go('program-detail',{pg});}} style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 10px', borderRadius:8, cursor:'pointer' }}>
										<Icon n="search" size={16} color={T.textTri}/>
										<div style={{ flex:1, minWidth:0 }}>
											<div style={{ fontSize:15, color:T.text }}>{hi(pg.title)}</div>
											<div style={{ fontSize:12, color:T.textTri }}>{pg.center} · {pg.region}</div>
										</div>
										<Badge text={pg.status} variant={pg.status==='마감'?'muted':pg.status==='진행예정'?'secondary':'primary'}/>
									</div>
								))}
							</div>}
					</div>
				) : (
					<>
						{recent.length>0 && <div style={{ marginBottom:32 }}>
							<div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:14 }}>
								<span style={{ fontSize:15, fontWeight:700, color:T.text }}>최근 검색어</span>
								<span className="btn-hover" onClick={()=>setRecent([])} style={{ fontSize:12.5, color:T.textTri, cursor:'pointer' }}>전체 삭제</span>
							</div>
							<div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
								{recent.map(k=>(
									<div key={k} style={{ display:'flex', alignItems:'center', gap:6, padding:'7px 10px 7px 14px', borderRadius:T.tagR, background:T.surface, border:`1px solid ${T.border}` }}>
										<span className="btn-hover" onClick={()=>{setQ(k);}} style={{ fontSize:13, color:T.textSec, cursor:'pointer' }}>{k}</span>
										<div className="btn-hover" onClick={()=>setRecent(r=>r.filter(x=>x!==k))} style={{ cursor:'pointer', display:'flex' }}><Icon n="close" size={13} color={T.textTri}/></div>
									</div>
								))}
							</div>
						</div>}
						<div>
							<span style={{ fontSize:15, fontWeight:700, color:T.text, display:'block', marginBottom:14 }}>인기 검색어</span>
							<div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:'2px 40px' }}>
								{POPULAR_KEYWORDS.map((k,i)=>(
									<div key={k} className="btn-hover" onClick={()=>{setQ(k);}} style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 8px', borderRadius:8, cursor:'pointer' }}>
										<span style={{ fontSize:15, fontWeight:700, color:i<3?T.primary:T.textTri, fontFamily:'Inter,sans-serif', width:18 }}>{i+1}</span>
										<span style={{ fontSize:14, color:T.text }}>{k}</span>
									</div>
								))}
							</div>
						</div>
					</>
				)}
			</div>
			<Footer/>
		</div>
	);
}

// ── 403 FORBIDDEN (관리자 영역 접근 차단) ──
function Forbidden403({ go }) {
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:20, padding:'72px 80px' }}>
				<div style={{ width:88, height:88, borderRadius:'50%', background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', marginBottom:4 }}>
					<svg viewBox="0 0 24 24" style={{ width:44, height:44 }}>
						<path d="M12 2l7 3v6c0 4.5-3 8.3-7 9.5C8 19.3 5 15.5 5 11V5l7-3z" stroke={T.primary} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>
						<rect x="9" y="10.5" width="6" height="5" rx="1.2" stroke={T.primary} strokeWidth="1.5" fill="none"/>
						<path d="M10.3 10.5V9.3a1.7 1.7 0 013.4 0v1.2" stroke={T.primary} strokeWidth="1.5" fill="none"/>
					</svg>
				</div>
				<div style={{ fontSize:24, fontWeight:700, color:T.text }}>접근 권한이 없습니다</div>
				<div style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, wordBreak:'keep-all' }}>
					이 페이지는 <strong style={{color:T.text}}>관리자 전용</strong> 영역으로, 일반 회원은 접근할 수 없어요.<br/>
					권한이 필요하시면 아래 연락처로 관리자에게 문의해주세요.
				</div>
				<div style={{ display:'flex', alignItems:'center', gap:14, padding:'16px 22px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, marginTop:6 }}>
					<div style={{ width:42, height:42, borderRadius:10, background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
						<Icon n="mail" size={20} color={T.primary}/>
					</div>
					<div>
						<div style={{ fontSize:12, color:T.textTri, marginBottom:2 }}>관리자 문의</div>
						<div style={{ fontSize:14, fontWeight:600, color:T.text }}>helpmoa@naver.com · 031-123-4567</div>
					</div>
				</div>
				<div style={{ display:'flex', gap:12, marginTop:18 }}>
					<Btn size="l" variant="ghost" onClick={()=>go('home')} style={{ width:160 }}>이전 페이지</Btn>
					<Btn size="l" variant="primary" onClick={()=>go('home')} style={{ width:160 }}>홈으로</Btn>
				</div>
				<span style={{ fontSize:12, color:T.textTri, marginTop:8 }}>Error code: 403 Forbidden</span>
			</div>
			<Footer/>
		</div>
	);
}

// ── SYSTEM: 404 / 503 / 세션 만료 ──
function SysLayout({ children }) {
	return (
		<div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
			<div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:20, padding:'72px 80px' }}>
				{children}
			</div>
			<Footer/>
		</div>
	);
}
function Error404({ go }) {
	return (
		<SysLayout>
			<div style={{ width:88, height:88, borderRadius:'50%', background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', marginBottom:4 }}>
				<svg viewBox="0 0 24 24" style={{ width:44, height:44 }}>
					<circle cx="11" cy="11" r="7" stroke={T.primary} strokeWidth="1.5" fill="none"/>
					<path d="M20.5 20.5l-4-4" stroke={T.primary} strokeWidth="1.6" fill="none" strokeLinecap="round"/>
					<path d="M11 8v3M11 13.5h.01" stroke={T.primary} strokeWidth="1.8" fill="none" strokeLinecap="round"/>
				</svg>
			</div>
			<div style={{ fontSize:24, fontWeight:700, color:T.text }}>페이지를 찾을 수 없습니다</div>
			<div style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, wordBreak:'keep-all' }}>
				요청하신 페이지가 삭제되었거나 주소가 변경되었어요.<br/>입력하신 주소가 정확한지 다시 확인해주세요.
			</div>
			<div style={{ display:'flex', gap:12, marginTop:18 }}>
				<Btn size="l" variant="ghost" onClick={()=>go('home')} style={{ width:150 }}>이전 페이지</Btn>
				<Btn size="l" variant="primary" onClick={()=>go('home')} style={{ width:150 }}>홈으로</Btn>
			</div>
			<span style={{ fontSize:12, color:T.textTri, marginTop:8 }}>Error code: 404 Not Found</span>
		</SysLayout>
	);
}
function Error503({ go }) {
	return (
		<SysLayout>
			<div style={{ width:88, height:88, borderRadius:'50%', background:'#FFF7ED', display:'flex', alignItems:'center', justifyContent:'center', marginBottom:4 }}>
				<svg viewBox="0 0 24 24" style={{ width:44, height:44 }}>
					<path d="M14.7 6.3a4 4 0 00-5.4 5.4l-6.3 6.3a1.5 1.5 0 000 2.1l.9.9a1.5 1.5 0 002.1 0l6.3-6.3a4 4 0 005.4-5.4l-2.3 2.3-2.1-.6-.6-2.1 2.3-2.3z" stroke={T.secondary} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>
				</svg>
			</div>
			<div style={{ fontSize:24, fontWeight:700, color:T.text }}>서비스 점검 중입니다</div>
			<div style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, wordBreak:'keep-all' }}>
				보다 나은 서비스를 제공하기 위해 시스템 점검을 진행하고 있어요.<br/>
				점검 시간 동안에는 이용이 제한되며, 완료 후 정상적으로 이용하실 수 있습니다.
			</div>
			<div style={{ display:'flex', alignItems:'center', gap:14, padding:'16px 22px', borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, marginTop:6 }}>
				<div style={{ width:42, height:42, borderRadius:10, background:'#FFF7ED', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
					<Icon n="calendar" size={20} color={T.secondary}/>
				</div>
				<div>
					<div style={{ fontSize:12, color:T.textTri, marginBottom:2 }}>점검 시간</div>
					<div style={{ fontSize:14, fontWeight:600, color:T.text }}>2026-06-25 (목) 02:00 ~ 06:00 (4시간)</div>
				</div>
			</div>
			<div style={{ display:'flex', gap:12, marginTop:18 }}>
				<Btn size="l" variant="primary" icon="refresh" onClick={()=>go('home')} style={{ width:180 }}>새로고침</Btn>
			</div>
			<span style={{ fontSize:12, color:T.textTri, marginTop:8 }}>문의 helpmoa@naver.com · 031-123-4567</span>
		</SysLayout>
	);
}
function SessionExpired({ go }) {
	return (
		<SysLayout>
			<div style={{ width:88, height:88, borderRadius:'50%', background:T.primaryBg, display:'flex', alignItems:'center', justifyContent:'center', marginBottom:4 }}>
				<svg viewBox="0 0 24 24" style={{ width:44, height:44 }}>
					<circle cx="12" cy="12" r="9" stroke={T.primary} strokeWidth="1.5" fill="none"/>
					<path d="M12 7.5V12l3 2" stroke={T.primary} strokeWidth="1.6" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
				</svg>
			</div>
			<div style={{ fontSize:24, fontWeight:700, color:T.text }}>로그인이 만료되었습니다</div>
			<div style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, wordBreak:'keep-all' }}>
				일정 시간 동안 활동이 없어 자동으로 로그아웃되었어요.<br/>
				계속 이용하시려면 다시 로그인해주세요.
			</div>
			<div style={{ display:'flex', gap:12, marginTop:18 }}>
				<Btn size="l" variant="ghost" onClick={()=>go('home')} style={{ width:150 }}>홈으로</Btn>
				<Btn size="l" variant="primary" onClick={()=>go('login')} style={{ width:180 }}>다시 로그인</Btn>
			</div>
		</SysLayout>
	);
}

// ── MAIN APP ──
function App() {
	const [screen, setScreen] = useState('home');
	const [ctx, setCtx] = useState({});
	const [isLoggedIn, setIsLoggedIn] = useState(true);
	const [toast, setToast] = useState(null);
	const [showLoginModal, setShowLoginModal] = useState(false);
	const [scrolled, setScrolled] = useState(false);
	const prevScroll = useRef(0);

	useEffect(() => { document.querySelector('.scroll-container')?.scrollTo(0,0); setScrolled(false); }, [screen]);

	const go = useCallback((s, c={}) => {
		prevScroll.current = window.scrollY;
		setScreen(s);
		setCtx(c);
		window.__ymGo = (ss,cc)=>{ setScreen(ss); setCtx(cc||{}); window.scrollTo({top:0,behavior:'smooth'}); };
		window.scrollTo({ top:0, behavior:'smooth' });
	}, []);
	React.useEffect(()=>{ window.__ymGo = (ss,cc)=>{ setScreen(ss); setCtx(cc||{}); window.scrollTo({top:0,behavior:'smooth'}); }; }, []);

	const addToast = useCallback((msg) => {
		setToast(msg);
	}, []);

	const onLoginClick = () => {
		go('login');
	};

	const noHeader = ['login','signup','find-id','welcome','error-503','session-expired'];
	const showHeader = !noHeader.includes(screen);

	const pages = {
		home: <HomeScreen go={go} isLoggedIn={isLoggedIn}/>,
		programs: <ProgramList go={go}/>,
		'program-detail': <ProgramDetail go={go} pg={ctx.pg||PROGRAMS[0]} isLoggedIn={isLoggedIn} onLoginClick={onLoginClick} addToast={addToast}/>,
		'program-apply': <ProgramApply go={go} pg={ctx.pg||PROGRAMS[0]} addToast={addToast}/>,
		'apply-complete': <ApplyComplete go={go} pg={ctx.pg||PROGRAMS[0]}/>,
		mypage: <MyPage go={go} addToast={addToast} initialTab={ctx.tab}/>,
		'application-detail': <ApplicationDetail go={go} pg={ctx.pg} addToast={addToast}/>,
		notices: <NoticesScreen go={go}/>,
		'notice-detail': <NoticeDetail go={go} notice={ctx.notice}/>,
		centers: <CentersScreen go={go} addToast={addToast}/>,
		login: <LoginScreen go={go} onLogin={()=>setIsLoggedIn(true)}/>,
		signup: <SignupScreen go={go} onLogin={()=>setIsLoggedIn(true)}/>,
		welcome: <WelcomeScreen go={go} addToast={addToast}/>,
		'find-id': <LoginScreen go={go} onLogin={()=>setIsLoggedIn(true)}/>,
		search: <SearchScreen go={go} initialQ={ctx.q}/>,
		forbidden: <Forbidden403 go={go}/>,
		'error-404': <Error404 go={go}/>,
		'error-503': <Error503 go={go}/>,
		'session-expired': <SessionExpired go={go}/>,
	};

	// Flow indicator
	const FLOW = { home:'홈', programs:'프로그램 목록', 'program-detail':'프로그램 상세', 'program-apply':'프로그램 신청', 'apply-complete':'신청 완료', mypage:'마이페이지', 'application-detail':'신청 상세', notices:'공지사항', 'notice-detail':'공지사항 상세', centers:'청년센터 찾기', search:'검색', login:'로그인', welcome:'회원가입 환영', forbidden:'403 접근 권한 없음', 'error-404':'404 페이지 없음', 'error-503':'503 점검 중', 'session-expired':'세션 만료' };

	return (
		<div style={{ minHeight:'100vh', background:'#0F0F13', display:'flex', flexDirection:'column', alignItems:'center' }}>
			{/* Browser chrome simulation */}
			<div style={{ width:'100%', maxWidth:1440, background:T.surface, minHeight:'100vh', position:'relative', boxShadow:'0 0 60px rgba(0,0,0,0.4)' }}>
				{/* Page (헤더를 스크롤 컨테이너 안 sticky로 두어 hero가 헤더 뒤 0px부터 시작) */}
				<div key={screen} className="scroll-container" onScroll={e=>setScrolled(e.currentTarget.scrollTop>60)} style={{ overflowY:'auto', maxHeight:'calc(100vh - 0px)' }}>
					{showHeader && (
						<div style={{ position:'sticky', top:0, zIndex:200 }}>
							<Header screen={screen} go={go} isLoggedIn={isLoggedIn} onLoginClick={onLoginClick} onLogout={()=>{setIsLoggedIn(false);addToast('로그아웃 되었습니다.');go('home');}} onMyPage={()=>go('mypage')} scrolled={scrolled}/>
						</div>
					)}
					{pages[screen]||<Error404 go={go}/>}
				</div>
			</div>
			{/* Toast */}
			{toast && <Toast msg={toast} onDone={()=>setToast(null)}/>}
			{/* Flow HUD */}
			<div style={{ position:'fixed', bottom:20, right:20, background:'rgba(0,0,0,0.8)', borderRadius:10, padding:'10px 16px', zIndex:9999, backdropFilter:'blur(8px)' }}>
				<div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:4 }}>현재 화면</div>
				<div style={{ fontSize:13, fontWeight:600, color:'#fff' }}>{FLOW[screen]||screen}</div>
			</div>
		</div>
	);
}

const rootEl = document.getElementById('root');
if (rootEl) createRoot(rootEl).render(<App/>);
export default App;

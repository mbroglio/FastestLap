import json

events = [
    {"timeSec": 0.0, "lap": 1, "text": "🏁 11:03:51 — Semafori spenti! Tutti i 22 piloti scattano dalla griglia di Baku per il GP dell'Azerbaigian!"},
    {"timeSec": 6.0, "lap": 1, "text": "⚡ 11:03:57 — Russell tiene la testa su Leclerc e Piastri alla staccata di Curva 1!"},
    {"timeSec": 250.0, "lap": 3, "text": "🏎️ 11:08:01 G3: Verstappen (#3) supera Norris (#1) con DRS a 338 km/h sul rettifilo di Neftchilar Avenue!"},
    {"timeSec": 580.0, "lap": 6, "text": "🏎️ 11:13:31 G6: Verstappen supera Hadjar (#6) e sale in P4 alla staccata di Curva 1!"},
    {"timeSec": 890.0, "lap": 8, "text": "⚠️ 11:18:41 G8: Ritiro per Lance Stroll (#18) — Sospensione danneggiata a Curva 15."},
    {"timeSec": 1200.0, "lap": 11, "text": "🏎️ 11:23:51 G11: Piastri (#81) sorpassa Leclerc (#16) e sale in 2ª posizione sul rettilineo d'arrivo!"},
    {"timeSec": 1650.0, "lap": 15, "text": "🏎️ 11:31:21 G15: Verstappen (#3) attacca e supera Leclerc (#16) conquistando il podio virtuale (P3)!"},
    {"timeSec": 2226.0, "lap": 20, "text": "🔧 11:40:57 G20: Pit stop per Carlos Sainz (#55) — Sosta regolare di 20.6s e rientro su Hard."},
    {"timeSec": 2310.0, "lap": 21, "text": "⚠️ 11:42:21 G21: Ritiro per Fernando Alonso (#14) ai box per surriscaldamento all'impianto frenante."},
    {"timeSec": 2700.0, "lap": 25, "text": "🏎️ 11:48:51 G25: Verstappen supera Piastri per la P2 e si lancia all'inseguimento del battistrada Russell!"},
    {"timeSec": 3280.0, "lap": 30, "text": "💥 11:58:31 G30: Incidente per Alex Albon (#23) alle barriere di Curva 15! Vettura ferma in traiettoria."},
    {"timeSec": 3285.0, "lap": 30, "text": "🟡 11:58:36 G30: SAFETY CAR IN PISTA! Corsia box affollata: pit stop per Antonelli, Norris, Gasly e Bearman!"},
    {"timeSec": 3925.0, "lap": 35, "text": "🟢 12:09:15 G35: Safety Car in this lap — Russell prepara lo strappo prima del traguardo!"},
    {"timeSec": 4044.0, "lap": 35, "text": "⚡ 12:11:14 G35: Ripartenza! Russell difende la P1 da Verstappen, duello serrato a Curva 1!"},
    {"timeSec": 4120.0, "lap": 36, "text": "💥 12:12:31 G36: Collisione a Curva 1 tra Lando Norris (#1) e Pierre Gasly (#10)! Entrambi costretti al ritiro!"},
    {"timeSec": 4130.0, "lap": 36, "text": "🟡 12:12:41 G36: SAFETY CAR 2 DEPLOYED per rimuovere le due vetture ferme a Curva 1!"},
    {"timeSec": 4210.0, "lap": 37, "text": "⚠️ 12:14:01 G37: Ritiro anche per Franco Colapinto (#43) ai box per avaria idraulica."},
    {"timeSec": 4396.0, "lap": 38, "text": "🟢 12:17:06 G38: Safety Car rientra ai box — Bandiera Verde e sprint finale di 13 giri!"},
    {"timeSec": 4600.0, "lap": 40, "text": "⚔️ 12:20:30 G40: Duello entusiasmante Russell-Verstappen! Distacco minimo di 0.25s lungo il rettilineo da 340 km/h!"},
    {"timeSec": 5350.0, "lap": 47, "text": "👑 12:33:00 G47: SORPASSO PER LA LEADERSHIP! Verstappen supera Russell all'esterno di Curva 1 e passa al comando!"},
    {"timeSec": 5480.0, "lap": 48, "text": "🟣 12:35:10 G48: Verstappen sigla il giro veloce in 1:44.993!"},
    {"timeSec": 5600.0, "lap": 49, "text": "🟣 12:37:10 G49: Russell risponde con il record assoluto della gara in 1:44.916 e si riattacca agli scarichi di Max!"},
    {"timeSec": 5881.0, "lap": 51, "text": "🏆 12:41:52 G51: Max Verstappen VINCE il GP dell'Azerbaigian in volata per +0.003s su George Russell! 3° Hadjar!"},
    {"timeSec": 6000.0, "lap": 51, "text": "🛑 12:43:51 G51: Vetture in Parc Fermé. Fine sessione ufficiale Baku 2026."}
]

with open('signalr_test_suite/data/baku_race_events.json', 'w', encoding='utf-8') as f:
    json.dump(events, f, indent=2)

print(f"Saved {len(events)} Baku race events to signalr_test_suite/data/baku_race_events.json")

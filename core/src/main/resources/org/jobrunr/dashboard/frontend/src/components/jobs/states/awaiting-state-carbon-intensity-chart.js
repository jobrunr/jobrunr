import React, {useEffect, useState} from 'react'

const hours = Array.from({length: 24}, (_, i) =>
    String(i).padStart(2, '0'),
)
const colors = [
    "#00FF00", "#0BF400", "#16E900", "#21DE00", "#2CD300", "#37C800",
    "#43BC00", "#4EB100", "#59A600", "#649B00", "#6F9000", "#7A8500",
    "#857A00", "#906F00", "#9B6400", "#A65900", "#B14E00", "#BC4300",
    "#C83700", "#D32C00", "#DE2100", "#E91600", "#F30C00", "#FF0000"
]

const CarbonIntensityChart = ({jobState}) => {
    const [ranks, setRanks] = useState(null)   // { "00": 5, "01": 3, ... }
    const [bestHour, setBestHour] = useState(null)
    const [windowRange, setWindowRange] = useState([0, 0])

    const buildForecastAndSetBestHour = (forecast) => {
        // 1) bucket all points into hours → pick min rank per hour
        const byHour = {}
        forecast.forEach(({periodStartAt, rank}) => {
            const h = new Date(periodStartAt).getUTCHours()
            const key = String(h).padStart(2, '0')
            if (!byHour[key]) byHour[key] = []
            byHour[key].push(rank)
        })
        const rawRanks = {}
        for (let i = 0; i < 24; i++) {
            const key = String(i).padStart(2, '0')
            rawRanks[key] = byHour[key]
                ? Math.min(...byHour[key])
                : null
        }

        // 2) normalize rawRanks[h] from [rawMin..rawMax] → [0..colors.length-1]
        const vals = Object.values(rawRanks).filter(v => v !== null)
        const rawMin = Math.min(...vals)
        const rawMax = Math.max(...vals)
        const N = colors.length - 1
        const norm = {}
        Object.entries(rawRanks).forEach(([h, r]) => {
            if (r === null) {
                norm[h] = null
            } else {
                const t = (r - rawMin) / (rawMax - rawMin)
                norm[h] = Math.round(t * N)
            }
        })

        // set the normalized ranks for coloring
        setRanks(norm)

        // 3) compute window bounds
        const fromH = new Date(jobState.from).getUTCHours()
        const toH = new Date(jobState.to).getUTCHours()
        setWindowRange([fromH, toH])

        // 4) find best hour by raw rank within the window
        let best = null, bestR = Infinity
        Object.entries(rawRanks).forEach(([h, r]) => {
            if (r === null) return
            const hi = Number(h)
            const inside =
                fromH <= toH
                    ? hi >= fromH && hi < toH
                    : hi >= fromH || hi < toH
            if (inside && r < bestR) {
                bestR = r
                best = h
            }
        })
        setBestHour(best);
    }

    useEffect(() => {
        fetch('/api/metadata/carbon-intensity-forecast')
            .then(r => r.json())
            .then(payload => {
                const rawValue = payload[0].value;
                const value = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
                console.log(value)
                buildForecastAndSetBestHour(value.intensityForecast);
            })
    }, [jobState]);

    if (!ranks) return null;
    const [fromH, toH] = windowRange;

    return (
        <div style={{width: '100%', fontFamily: 'sans-serif', marginTop: '2.5em'}}>
            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(24, 1fr)',
                    gridTemplateRows: '2px 16px 16px',
                    gridAutoFlow: 'column',    // ← add this
                    rowGap: '4px',
                    marginBottom: '8px'
                }}
            >
                {hours.map((h, idx) => {
                    const rank = ranks[h];
                    const inWindow = fromH <= toH
                        ? idx >= fromH && idx < toH
                        : idx >= fromH || idx < toH;
                    const isBest = h === bestHour;
                    const baseCol = colors[rank];
                    return (
                        <React.Fragment key={h}>
                            <div style={{background: inWindow ? 'transparent' : '#E53935'}}/>
                            <div style={{
                                position: 'relative',
                                background: baseCol,
                                opacity: inWindow ? 1 : 0.6,
                                transform: isBest ? 'scaleY(1.2)' : 'scaleY(1)',
                                transformOrigin: 'center',
                                boxSizing: 'border-box',
                                width: '100%',
                                height: '100%'
                            }}
                                 title={`Hour ${h}: rank ${rank}${inWindow ? '' : ' (unavailable)'}`}
                            >
                                {isBest && (
                                    <div
                                        style={{
                                            position: 'absolute',
                                            top: '-14px',
                                            left: '50%',
                                            transform: 'translateX(-50%)',
                                            width: 0,
                                            height: 0,
                                            borderLeft: '6px solid transparent',
                                            borderRight: '6px solid transparent',
                                            borderTop: '12px solid #fbe200'
                                        }}
                                    />
                                )}
                            </div>
                            <div style={{textAlign: 'center', fontSize: '10px', color: '#333'}}>{h}</div>
                        </React.Fragment>
                    );
                })}
            </div>

            <div style={{display: 'flex', alignItems: 'center', gap: '16px', fontSize: '12px'}}>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div
                        style={{
                            width: 0,
                            height: 0,
                            borderLeft: '6px solid transparent',
                            borderRight: '6px solid transparent',
                            borderTop: '12px solid #fbe200',
                        }}
                    />
                    <span>Best hour (rank {ranks[bestHour]})</span>
                </div>

                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div style={{width: '16px', height: '2px', background: '#E53935'}}/>
                    <span>Outside execution window</span>
                </div>
            </div>
        </div>
    );
}

export default CarbonIntensityChart;
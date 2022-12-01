import React from 'react';
import Chip from "@material-ui/core/Chip";

const JobLabel = (props) => {
    let backgroundColor = getRandomColor(props?.text);
    let foregroundColor = getContrastYIQ(backgroundColor);
    return (
        <Chip label={props?.text}
              style={{backgroundColor: backgroundColor, color: foregroundColor, marginRight: '0.5rem', height: '22px'}}
              size="small"/>
    );
}

function getRandomColor(text) {
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
        hash = text.charCodeAt(i) + ((hash << 5) - hash);
    }
    const c = (hash & 0x00FFFFFF)
        .toString(16)
        .toUpperCase();

    return "#" + "00000".substring(0, 6 - c.length) + c;
}

function getContrastYIQ(hexColor) {
    const hexColorWithoutHash = hexColor.replace("#", "");
    const r = parseInt(hexColorWithoutHash.substring(0, 2), 16);
    const g = parseInt(hexColorWithoutHash.substring(2, 4), 16);
    const b = parseInt(hexColorWithoutHash.substring(4, 6), 16);
    const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
    return (yiq >= 128) ? 'black' : 'white';
}

export default JobLabel;
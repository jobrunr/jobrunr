import Chip from "@mui/material/Chip";
import {stringToColor} from "../../utils/helper-functions.js";

const JobLabel = (props) => {
    let backgroundColor = stringToColor(props?.text);
    let foregroundColor = getContrastYIQ(backgroundColor);
    return (
        <Chip label={props?.text}
              style={{backgroundColor: backgroundColor, color: foregroundColor, marginRight: '0.5rem', height: '22px'}}
              size="small"/>
    );
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
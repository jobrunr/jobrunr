import Chip from "@mui/material/Chip";

export const StatChip = ({label: stat, ...rest}) => {
    const renderedStat = stat === undefined ? "?" : stat;
    return <Chip label={renderedStat} {...rest} />
}
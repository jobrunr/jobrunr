import Typography from "@mui/material/Typography";

export const ItemsNotFound = ({children, ...rest}) => (
    <Typography variant="body1" style={{padding: '1rem'}} {...rest}>
        {children}
    </Typography>
);
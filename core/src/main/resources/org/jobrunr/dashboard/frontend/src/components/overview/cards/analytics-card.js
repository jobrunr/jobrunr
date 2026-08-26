import {alpha} from "@mui/material";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

export const AnalyticsCard = ({title, children, icon: Icon, color = "primary", textId = undefined}) => {
    return (
        <Card variant="outlined" sx={{minWidth: 215, height: "100%"}}>
            <CardContent sx={{px: 2.5, py: (theme) => `${theme.spacing(2)} !important`}}>
                <Box sx={{display: "flex", alignItems: "center", gap: 1}}>
                    <Box
                        sx={(theme) => ({
                            width: 28,
                            height: 28,
                            flexShrink: 0,
                            borderRadius: 999,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            backgroundColor: alpha(theme.palette[color].main, 0.12),
                        })}
                    >
                        <Icon color={color} fontSize="tiny"/>
                    </Box>
                    <Typography variant="body2" color="text.secondary">
                        {title}
                    </Typography>
                </Box>

                <Box sx={{overflowX: "hidden", flexGrow: 1, display: "flex", gap: 2, alignItems: "center", justifyContent: "space-between", mt: 1}}>
                    <Typography
                        sx={{
                            opacity: 0.85,
                            fontSize: "1.5rem",
                            fontWeight: 600,
                            whiteSpace: "nowrap",
                            fontVariantNumeric: "tabular-nums",
                            overflowX: "hidden",
                            textOverflow: "ellipsis",
                            display: "block",
                            width: "100%",
                        }}
                        variant="span"
                        id={textId}
                    >
                        {children}
                    </Typography>
                </Box>
            </CardContent>
        </Card>
    );
}
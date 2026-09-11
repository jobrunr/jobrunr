import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

export const AnalyticsCard = ({
                                  title,
                                  children,
                                  icon: Icon,
                                  color = "primary",
                                  sparkline = undefined,
                                  textId = undefined,
                                  subtitle = undefined
                              }) => {
    return (
        <Card variant="outlined" sx={{minWidth: 215, height: "100%"}}>
            <CardContent sx={{px: 2, pt: (theme) => `${theme.spacing(1.5)} !important`, pb: (theme) => `${theme.spacing(1)} !important`}}>
                <Box sx={{display: "flex", alignItems: "center", flexGrow: 1, justifyContent: "space-between", gap: 1, opacity: 0.8}}>
                    <Box sx={{display: "flex", alignItems: "center", gap: 1, flexGrow: 1, justifyContent: "space-between"}}>
                        <Typography variant="caption" color="text.secondary" sx={{display: "flex", gap: 0.5, alignItems: "center"}}>
                            {title}
                        </Typography>
                        <Icon color={color} fontSize="tiny"/>
                    </Box>
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
                <Box sx={{width: "100%", overflow: "hidden", minWidth: 0}}>
                    <Typography variant="caption" color="text.secondary"
                                sx={{opacity: 0.85, display: "block", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", my: 0.5}}>
                        {subtitle}
                        &nbsp;
                    </Typography>
                </Box>
                <Box sx={{width: "100%"}}>
                    {sparkline && <Box sx={{display: "flex", alignItems: "center", flexDirection: "column"}}>
                        {sparkline}
                    </Box>}
                </Box>
            </CardContent>
        </Card>
    );
}
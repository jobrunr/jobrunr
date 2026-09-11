import {SuffixFreeTimeAgo, SwitchableTimeFormatter} from "../../utils/time-ago";
import {TimerOutlined} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";

const UptimeCard = ({servers}) => {
    return (
        <AnalyticsCard title="Uptime" icon={TimerOutlined}
                       subtitle={<>Since <SwitchableTimeFormatter date={new Date(servers[0].firstHeartbeat)}/></>}>
            <SuffixFreeTimeAgo date={new Date(servers[0].firstHeartbeat)}/>
        </AnalyticsCard>
    );
};

export default UptimeCard;
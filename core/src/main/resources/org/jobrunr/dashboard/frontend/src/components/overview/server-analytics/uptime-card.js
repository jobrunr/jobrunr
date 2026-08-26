import {SuffixFreeTimeAgo} from "../../utils/time-ago";
import {TimerOutlined} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";

const UptimeCard = ({servers}) => {
    return (
        <AnalyticsCard title="Uptime" icon={TimerOutlined}>
            <SuffixFreeTimeAgo date={new Date(servers[0].firstHeartbeat)}/>
        </AnalyticsCard>
    );
};

export default UptimeCard;
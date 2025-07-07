import {Link, useLocation, useNavigate} from "react-router-dom";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TablePagination from '@mui/material/TablePagination';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import LoadingIndicator from "../LoadingIndicator";
import JobLabel from "../utils/job-label";
import {ItemsNotFound} from "../utils/items-not-found";
import {styled} from "@mui/material/styles";
import {SwitchableTimeFormatter, SwitchableTimeRangeFormatter} from "../utils/time-ago";
import Tooltip from '@mui/material/Tooltip';
import {EnergySavingsLeaf} from "@mui/icons-material";
import {getJobMostRecentState, getJobPreviousState, isCarbonAwaitingState} from "../utils/job-utils";

const IdColumn = styled(TableCell)`
    width: 20%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
`;

const JobsTable = ({jobPage, jobState, isLoading}) => {
    const location = useLocation();
    const navigate = useNavigate();

    let column;
    let columnFunction = (job) => getJobMostRecentState(job).createdAt || new Date().toISOString();
    switch (jobState) {
        case 'AWAITING':
            column = "Created";
            break;
        case 'SCHEDULED':
            column = "Scheduled";
            columnFunction = (job) => getJobMostRecentState(job).scheduledAt || new Date().toISOString();
            break;
        case 'ENQUEUED':
            column = "Enqueued";
            break;
        case 'PROCESSING':
            column = "Started";
            break;
        case 'SUCCEEDED':
            column = "Succeeded";
            break;
        case 'FAILED':
            column = "Failed";
            break;
        case 'DELETED':
            column = "Deleted";
            break;
        default:
        // code block
    }

    const handleChangePage = (event, newPage) => {
        let urlSearchParams = new URLSearchParams(location.search);
        urlSearchParams.set("page", newPage);
        navigate(`?${urlSearchParams.toString()}`);
    };

    const isInAwaitingViewAndJobIsCarbonAware = (job) => jobState === "AWAITING" && isCarbonAwaitingState(getJobMostRecentState(job));
    const isInScheduledViewAndJobIsCarbonAware = (job) => jobState === "SCHEDULED" && isCarbonAwaitingState(getJobPreviousState(job));

    return (
        <> {isLoading
            ? <LoadingIndicator/>
            : <> {jobPage.items < 1
                ? <ItemsNotFound id="no-jobs-found-message">No jobs found</ItemsNotFound>
                : <>
                    <TableContainer>
                        <Table id="jobs-table" style={{width: "100%"}} aria-label="jobs table">
                            <TableHead>
                                <TableRow>
                                    <IdColumn>Id</IdColumn>
                                    <TableCell style={{width: '60%'}}>Job details</TableCell>
                                    <TableCell>{column}</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {jobPage.items.map(job => (
                                    <TableRow key={job.id}>
                                        <IdColumn component="th" scope="row">
                                            <Link to={{
                                                pathname: `/dashboard/jobs/${job.id}`,
                                                job: job
                                            }}>{job.id}</Link>
                                        </IdColumn>
                                        <TableCell>
                                            {job.labels &&
                                                <>
                                                    {job.labels.map((label) => <JobLabel key={label} text={label}/>)}
                                                    <span style={{marginRight: '0.5rem'}}></span>
                                                </>
                                            }
                                            <Link to={{
                                                pathname: `/dashboard/jobs/${job.id}`,
                                                job: job
                                            }}>{job.jobName}</Link>
                                        </TableCell>
                                        <TableCell>
                                            <div style={{display: "flex", alignItems: "center"}}>
                                                {isInAwaitingViewAndJobIsCarbonAware(job) &&
                                                    <Tooltip title={
                                                        <>
                                                            This is a Carbon Aware job that will be scheduled <SwitchableTimeRangeFormatter
                                                            from={new Date(getJobMostRecentState(job).from)}
                                                            to={new Date(getJobMostRecentState(job).to)}/>
                                                        </>
                                                    }>
                                                        <EnergySavingsLeaf fontSize="small" color="success" style={{marginRight: "4px"}}/>
                                                    </Tooltip>
                                                }
                                                {isInScheduledViewAndJobIsCarbonAware(job) &&
                                                    <Tooltip title="This is a Carbon Aware job">
                                                        <EnergySavingsLeaf fontSize="small" color="success" style={{marginRight: "4px"}}/>
                                                    </Tooltip>
                                                }
                                                <SwitchableTimeFormatter date={new Date(columnFunction(job))}/>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                    <TablePagination
                        id="jobs-table-pagination"
                        component="div"
                        rowsPerPageOptions={[]}
                        count={jobPage.total}
                        rowsPerPage={jobPage.limit}
                        page={jobPage.currentPage}
                        onPageChange={handleChangePage}
                    />
                </>
            }
            </>
        }
        </>
    );
}

export default JobsTable;
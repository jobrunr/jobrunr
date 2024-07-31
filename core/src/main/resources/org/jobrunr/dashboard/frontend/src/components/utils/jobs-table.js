import {Link, useLocation, useNavigate} from "react-router-dom";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TablePagination from '@mui/material/TablePagination';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TimeAgo from "react-timeago/lib";
import LoadingIndicator from "../LoadingIndicator";
import JobLabel from "./job-label";
import {ItemsNotFound} from "./items-not-found";
import {styled} from "@mui/material/styles";

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
    let columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].createdAt;
    switch (jobState) {
        case 'AWAITING':
            column = "Deadline";
            columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].to
            break;
        case 'SCHEDULED':
            column = "Scheduled";
            columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].scheduledAt;
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
                                                    {job.labels.map((label) => <JobLabel text={label}/>)}
                                                    <span style={{marginRight: '0.5rem'}}></span>
                                                </>
                                            }
                                            <Link to={{
                                                pathname: `/dashboard/jobs/${job.id}`,
                                                job: job
                                            }}>{job.jobName}</Link>
                                        </TableCell>
                                        <TableCell>
                                            <Link to={{pathname: `/dashboard/jobs/${job.id}`, job: job}}>
                                                <TimeAgo date={new Date(columnFunction(job))}
                                                         title={new Date(columnFunction(job)).toString()}/>
                                            </Link>
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
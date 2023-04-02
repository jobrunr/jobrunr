import React from 'react';
import {useHistory, useLocation} from "react-router-dom";
import Typography from '@material-ui/core/Typography';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Checkbox,
    Chip,
    FormControl,
    FormControlLabel,
    Grid,
    InputLabel,
    MenuItem,
    Select
} from "@material-ui/core";
import {ExpandMoreOutlined} from "@material-ui/icons";
import {makeStyles} from "@material-ui/core/styles";
import {DeleteForever} from "mdi-material-ui";
import SearchField from "../utils/search-field";
import serversState from "../../ServersStateContext";
import {KeyboardDateTimePicker} from "@material-ui/pickers";

const useStyles = makeStyles(theme => ({
    heading: {
        fontSize: theme.typography.pxToRem(15),
        flexBasis: '8%',
        flexShrink: 0,
        margin: theme.spacing(1)
    },
    secondaryHeading: {
        fontSize: theme.typography.pxToRem(15),
        color: theme.palette.text.secondary,
    },
    filter: {
        margin: theme.spacing(0.5)
    },
    noFiltersSelected: {
        display: 'inline-block',
        margin: theme.spacing(1)
    }
}));

const RecurringJobsFilterPanel = () => {
    const classes = useStyles();
    const location = useLocation();
    const history = useHistory();

    const queues = ["High Prio", "Default", "Low Prio", "Very Low Prio"];
    const [recurringJobSignatures, setRecurringJobSignatures] = React.useState([]);
    const [servers, setServers] = React.useState(serversState.getServers());

    React.useEffect(() => {
        fetch(`/api/job-signatures`)
            .then(res => res.json())
            .then(response => {
                setRecurringJobSignatures(response);
            })
            .catch(error => console.log(error));
    }, []);

    React.useEffect(() => {
        serversState.addListener(setServers);
        return () => serversState.removeListener(setServers);
    }, []);

    const urlSearchParams = new URLSearchParams(location.search);

    const setFilter = (filterName, filterValue, hasError) => {
        urlSearchParams.delete('action');
        if (filterValue !== null && filterValue.length !== 0) {
            if (hasError) {
                urlSearchParams.delete(filterName);
                urlSearchParams.set(filterName + 'Error', true);
            } else {
                urlSearchParams.set(filterName, filterValue);
                urlSearchParams.delete(filterName + 'Error');
            }

            history.push(`?${urlSearchParams.toString()}`);
        } else {
            removeFilter(filterName);
        }
    }

    const setDateFilter = (filterName, filterValue) => {
        let parsedDate = Date.parse(filterValue);
        if (!isNaN(parsedDate)) {
            filterValue.setUTCSeconds(0, 0);
            setFilter(filterName, filterValue.toISOString());
        }
    }

    const removeFilter = (filterName) => {
        urlSearchParams.delete('action');
        urlSearchParams.delete(filterName);
        history.push(`?${urlSearchParams.toString()}`);
    }

    const getActiveFilters = () => {
        const labelMapping = (urlParam) => {
            switch (urlParam) {
                case 'id':
                    return `Id: ${urlSearchParams.get(urlParam)}`;
                case 'jobName':
                    return `Job name: ${urlSearchParams.get(urlParam)}`;
                case 'jobSignature':
                    return `Job signature: ${urlSearchParams.get(urlParam)}`;
                case 'label':
                    return `Label: ${urlSearchParams.get(urlParam)}`;
                case 'priority':
                    return `Job queue: ${queues[urlSearchParams.get(urlParam)]}`;
                case 'serverTag':
                    return `Server tag: ${urlSearchParams.get(urlParam)}`;
                case 'paused':
                    if (urlSearchParams.get(urlParam) === "true") {
                        return `State: paused`;
                    } else {
                        return `State: running`;
                    }
                case 'runsMoreThanOncePerMinute':
                    if (urlSearchParams.get(urlParam) === "true") {
                        return `Frequency: more than once per minute`;
                    } else {
                        return `Frequency: less than once per minute`;
                    }
                case 'createdAtFrom':
                    return `Created after: ${urlSearchParams.get(urlParam)}`;
                case 'createdAtTo':
                    return `Created before: ${urlSearchParams.get(urlParam)}`;
                case 'updatedAtFrom':
                    return `Updated after: ${urlSearchParams.get(urlParam)}`;
                case 'updatedAtTo':
                    return `Updated before: ${urlSearchParams.get(urlParam)}`;
                case 'nextRunFrom':
                    return `Next run after: ${urlSearchParams.get(urlParam)}`;
                case 'nextRunTo':
                    return `Next run before: ${urlSearchParams.get(urlParam)}`;
                default:
                    return "Unknown filter";
            }
        }

        let filterKeys = Array.from(urlSearchParams.keys())
            .filter(key => !['state', 'page', 'queuePriority', 'itemsPerPage', 'action'].includes(key));

        return (
            <>
                {filterKeys.length > 0
                    ? <>
                        {filterKeys.map((key) => (
                            <Chip key={key}
                                  label={labelMapping(key)}
                                  onDelete={() => removeFilter(key)}
                                  deleteIcon={<DeleteForever/>}
                                  className={classes.filter}
                            />
                        ))}
                    </>
                    : <><span className={classes.noFiltersSelected}>No filters selected</span></>
                }
            </>
        )
    }

    return (
        <Accordion>
            <AccordionSummary
                expandIcon={<ExpandMoreOutlined/>}
                aria-controls="panel1a-content"
                id="panel1a-header"
            >
                <Typography className={classes.heading}>Filters</Typography>
                <div className={classes.secondaryHeading}>{getActiveFilters()}</div>
            </AccordionSummary>
            <AccordionDetails>
                <Grid container>
                    <Grid item xs={12} sm={12} lg={12} container spacing={3} style={{marginBottom: '1em'}}>
                        <Grid item xs={4}>
                            <SearchField id="job-id" label="Recurring job id" defaultValue={urlSearchParams.get('id') ?? ''}
                                         onSearch={value => setFilter('id', value)}/>
                        </Grid>
                        <Grid item xs={4}>
                            <SearchField id="job-name" label="Recurring job name"
                                         defaultValue={urlSearchParams.get('jobName') ?? ''}
                                         onSearch={value => setFilter('jobName', value)}/>
                        </Grid>
                        <Grid item xs={4}>
                            <FormControl style={{width: '100%'}}>
                                <InputLabel id="recurring-job-signature-select-label">Job signature</InputLabel>
                                <Select labelId="recurring-job-signature-select-label" id="recurring-job-signature-select"
                                        value={urlSearchParams.get('jobSignature') ?? ''}
                                        onChange={ev => setFilter('jobSignature', ev.target.value)}
                                >
                                    {recurringJobSignatures.map((jobSignature, index) => (
                                        <MenuItem key={index} value={jobSignature}>{jobSignature}</MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                    </Grid>
                    <Grid item xs={12} sm={12} lg={12} container spacing={3} style={{marginBottom: '1em'}}>
                        <Grid item xs={2}>
                            <FormControl style={{width: '100%'}}>
                                <FormControlLabel
                                    style={{paddingTop: '0.75em', color: 'rgba(0, 0, 0, 0.54)'}}
                                    control={
                                        <Checkbox
                                            onChange={e => {
                                                if (urlSearchParams.get('paused') === 'true') {
                                                    setFilter('paused', 'false');
                                                } else if (urlSearchParams.get('paused') === 'false') {
                                                    removeFilter('paused');
                                                } else {
                                                    setFilter('paused', 'true');
                                                }
                                            }}
                                            checked={urlSearchParams.get('paused') === "true"}
                                            indeterminate={!urlSearchParams.has('paused')}
                                            inputProps={{'aria-label': 'indeterminate checkbox'}}
                                        />
                                    }
                                    label="Paused jobs"></FormControlLabel>

                            </FormControl>
                        </Grid>
                        <Grid item xs={2}>
                            <FormControl style={{width: '100%'}}>
                                <FormControlLabel
                                    style={{paddingTop: '0.75em', color: 'rgba(0, 0, 0, 0.54)'}}
                                    control={
                                        <Checkbox
                                            onChange={e => {
                                                if (urlSearchParams.get('runsMoreThanOncePerMinute') === 'true') {
                                                    setFilter('runsMoreThanOncePerMinute', 'false');
                                                } else if (urlSearchParams.get('runsMoreThanOncePerMinute') === 'false') {
                                                    removeFilter('runsMoreThanOncePerMinute');
                                                } else {
                                                    setFilter('runsMoreThanOncePerMinute', 'true');
                                                }
                                            }}
                                            checked={urlSearchParams.get('runsMoreThanOncePerMinute') === "true"}
                                            indeterminate={!urlSearchParams.has('runsMoreThanOncePerMinute')}
                                            inputProps={{'aria-label': 'indeterminate checkbox'}}
                                        />
                                    }
                                    label="Runs more than once / min"></FormControlLabel>
                            </FormControl>
                        </Grid>
                        <Grid item xs={4}>
                            <SearchField id="job-label" label="Recurring job label" defaultValue={urlSearchParams.get('label') ?? ''}
                                         onSearch={value => setFilter('label', value)}/>
                        </Grid>
                        <Grid item xs={2}>
                            <FormControl style={{width: '100%'}}>
                                <InputLabel id="queue-select-label">Queue</InputLabel>
                                <Select labelId="queue-select-label" id="queue-select"
                                        value={urlSearchParams.get('priority') ?? ''}
                                        onChange={ev => setFilter('priority', ev.target.value)}
                                >
                                    {queues.map((queue, index) => (
                                        <MenuItem key={index} value={index}>{queue}</MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={2}>
                            <FormControl style={{width: '100%'}}>
                                <InputLabel id="server-tags-select-label">Server tag</InputLabel>
                                <Select labelId="server-tags-select-label" id="server-tags-select"
                                        value={urlSearchParams.get('serverTag') ?? ''}
                                        onChange={ev => setFilter('serverTag', ev.target.value)}
                                >
                                    {servers.flatMap(server => server.serverTags).map((serverTag, index) => (
                                        <MenuItem key={index} value={serverTag}>{serverTag}</MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                    </Grid>
                    <Grid item xs={12} sm={12} lg={12} container spacing={3} style={{marginBottom: '1em'}}>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Created after"
                                maxDate={urlSearchParams.get('createdAtTo')}
                                value={urlSearchParams.get('createdAtFrom')}
                                onChange={value => setDateFilter('createdAtFrom', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('createdAtFrom')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Created before"
                                minDate={urlSearchParams.get('createdAtFrom')}
                                value={urlSearchParams.get('createdAtTo')}
                                onChange={value => setDateFilter('createdAtTo', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('createdAtTo')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Updated after"
                                maxDate={urlSearchParams.get('updatedAtTo')}
                                value={urlSearchParams.get('updatedAtFrom')}
                                onChange={value => setDateFilter('updatedAtFrom', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('updatedAtFrom')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Updated before"
                                minDate={urlSearchParams.get('updatedAtFrom')}
                                value={urlSearchParams.get('updatedAtTo')}
                                onChange={value => setDateFilter('updatedAtTo', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('updatedAtTo')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Next run after"
                                minDate={new Date().toISOString()}
                                maxDate={urlSearchParams.get('nextRunTo')}
                                value={urlSearchParams.get('nextRunFrom')}
                                onChange={value => setDateFilter('nextRunFrom', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('nextRunFrom')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                        <Grid item xs={2}>
                            <KeyboardDateTimePicker
                                ampm={false}
                                label="Next run before"
                                minDate={urlSearchParams.get('nextRunFrom') || new Date().toISOString()}
                                value={urlSearchParams.get('nextRunTo')}
                                onChange={value => setDateFilter('nextRunTo', value)}
                                onKeyPress={ev => {
                                    if (ev.key === 'Enter') {
                                        removeFilter('nextRunTo')
                                    }
                                }}
                                format="yyyy/MM/dd HH:mm"
                                showTodayButton
                            />
                        </Grid>
                    </Grid>
                </Grid>

            </AccordionDetails>
        </Accordion>
    );
};

export default RecurringJobsFilterPanel;

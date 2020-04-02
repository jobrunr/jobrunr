import Grid from "@material-ui/core/Grid";
import Highlight from "react-highlight";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
    codeContent: {
        marginTop: 0,
        paddingTop: "0 !important",
        '& > pre': {
            marginTop: 0,
        }
    }
}));

const JobCode = (props) => {
    const { job } = props;
    const classes = useStyles();

    const fqClassName = job.jobDetails.className;
    const className = job.jobDetails.className.substring(job.jobDetails.className.lastIndexOf(".") + 1);
    const staticFieldName = job.jobDetails.staticFieldName;
    const methodName = job.jobDetails.methodName;
    const parameters = job.jobDetails.jobParameters.map(jobParameter => jobParameter.object).join(", ")

    let totalFunction = className;
    if(staticFieldName) {
        totalFunction += "." + staticFieldName;
    }
    totalFunction += "." + methodName;
    totalFunction += "(" + parameters + ")";


    const code = `
    import ${fqClassName};
    
    ${totalFunction};
    `;

    return (
        <Grid item xs={12} className={classes.codeContent}>
            <Highlight className='language-java'>
                {code}
            </Highlight>
        </Grid>
    );


};

export default JobCode;
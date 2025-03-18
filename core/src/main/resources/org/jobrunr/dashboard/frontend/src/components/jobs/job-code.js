import Grid from "@mui/material/Grid";
import Highlight from "../utils/highlighter";

const JobCode = (props) => {
    const {job} = props;

    const fqClassName = job.jobDetails.className;
    const className = job.jobDetails.className.substring(job.jobDetails.className.lastIndexOf(".") + 1);
    const staticFieldName = job.jobDetails.staticFieldName;
    const methodName = job.jobDetails.methodName;
    const parameters = job.jobDetails.jobParameters
        .map(jobParameter => jobParameter.object)
        .map(object => 
            typeof object === "object" ? JSON.stringify(object) : object
        )
        .join(", ")

    let totalFunction = className;
    if (staticFieldName) {
        totalFunction += "." + staticFieldName;
    }
    totalFunction += "." + methodName;
    totalFunction += "(" + parameters + ")";


    const code = `
    import ${fqClassName};
    
    ${totalFunction};
    `;

    return (
        <Grid item xs={12} sx={{marginTop: 0, paddingTop: "0 !important", '& > pre': {marginTop: 0}}}>
            <Highlight language="java">
                {code}
            </Highlight>
        </Grid>
    );


};

export default JobCode;
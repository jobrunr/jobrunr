import {Light as Highlight} from 'react-syntax-highlighter';
import java from 'react-syntax-highlighter/dist/esm/languages/hljs/java';
import yaml from 'react-syntax-highlighter/dist/esm/languages/hljs/yaml';
import style from 'react-syntax-highlighter/dist/esm/styles/hljs/androidstudio';

Highlight.registerLanguage('yaml', yaml);
Highlight.registerLanguage('java', java);

const Highlighter = ({language, children}) => (<Highlight language={language} style={style}>{children}</Highlight>);
export default Highlighter;
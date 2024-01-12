import {injectStyle, injectStyleSimple} from "@/Unstyled";
import bgImage from "@/Assets/Images/background_polygons.png";

export const GradientWithPolygons = injectStyle("polygon-background", k => `
    ${k} {
        background-image: url(${bgImage}), linear-gradient(var(--gradientStart), var(--gradientEnd));
        background-position: 0% 35%;
        background-repeat: repeat;
        padding-bottom: 75px;
        min-height: calc(100vh + 12px);
    }
    
    
    html.dark ${k} #search-icon,
    html.dark ${k} #refresh-icon {
        color: var(--textPrimary) !important;
    }
    
    
`);

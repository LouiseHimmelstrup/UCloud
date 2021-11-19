export const defaultModalStyle = {
    content: {
        borderRadius: "6px",
        top: "50%",
        left: "50%",
        right: "auto",
        bottom: "auto",
        marginRight: "-50%",
        transform: "translate(-50%, -50%)",
        background: "",
        minWidth: "500px",
        maxHeight: "80vh",
        border: "solid 1px var(--black)"
    },
    overlay: {
        backgroundColor: "var(--modalShadow)"
    }
};

export const largeModalStyle = {
    content: {
        borderRadius: "6px",
        top: "50%",
        left: "50%",
        right: "auto",
        bottom: "auto",
        marginRight: "-50%",
        transform: "translate(-50%, -50%)",
        background: "",
        minWidth: "800px",
        maxWidth: "1200px",
        minHeight: "400px",
        height: "80vh",
        border: "solid 1px var(--black)"
    },
    overlay: {
        backgroundColor: "var(--modalShadow)"
    }
};
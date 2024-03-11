import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
const 분실물 = () => {
    return (
        <>
            <button 
                type="button" 
                className="
                    grid
                    flex-col
                    justify-items-center

                    text-black 
                    bg-transparent 
                    hover:bg-transparent 
                    focus:outline-none 
                    focus:ring-4 
                    focus:bg-transparent 

                    dark:text-white
                    dark:bg-transparent 
                    dark:hover:bg-transparent 
                    dark:focus:bg-transparent

                    font-medium 
                    rounded-lg 
                    text-sm 
                    px-2.5 py-2.5 
                    text-center 
                    m-2 ">
                <ErrorOutlineIcon className="flex flex-auto"/>
                분실물
            </button>
        </>
    )
};

export default 분실물;
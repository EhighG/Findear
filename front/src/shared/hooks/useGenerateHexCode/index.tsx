const useGenerateHexCode = () => {
  let hexCode = "";
  const hexChars = "0123456789ABCDEF";

  for (let i = 0; i < 16; i++) {
    hexCode += hexChars.charAt(Math.floor(Math.random() * hexChars.length));
  }

  return hexCode;
};

export default useGenerateHexCode;

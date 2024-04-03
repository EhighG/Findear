const useGenerateHexCode = (fileName: string) => {
  let hexCode = "";
  const hexChars = "0123456789ABCDEF";

  for (let i = 0; i < 16; i++) {
    hexCode += hexChars.charAt(Math.floor(Math.random() * hexChars.length));
  }
  //  파일 네임에서 .확장자 까지 포함해서 넘겨주기
  return `${hexCode}.${fileName.split(".").pop()}`;
};

export default useGenerateHexCode;

const usePasswordValidation = (password: string) => {
  const regex =
    /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,16}$/;
  return regex.test(password);
};

export default usePasswordValidation;

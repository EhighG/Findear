import { Findear, Text, cls } from "@/shared";

type StampProps = {
  className?: string;
};

const FindearStamp = ({ className }: StampProps) => {
  return (
    <div
      className={cls(
        className ? className : "",
        "flex flex-col items-center border border-A706Dark rounded-full p-2 rotate-45 absolute size-[90px]"
      )}
    >
      <>
        <img src={Findear} alt="Findear" className="size-[50px]" />
        <Text>인계 완료</Text>
      </>
    </div>
  );
};

export default FindearStamp;

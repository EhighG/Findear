type ProgressProps = {
  progress: number;
};

const MAX_WIDTH = 300;
const MAX_WIDTH_CLASS_NAME = "w-[" + MAX_WIDTH + "px]";
const widths: Record<number, string> = {
  0: "w-[0px]",
  1: "w-[43px]",
  2: "w-[86px]",
  3: "w-[129px]",
  4: "w-[172px]",
  5: "w-[215px]",
  6: "w-[258px]",
  7: "w-[300px]",
};
// const widths = new Array(7)
//   .fill(0)
//   .map((_, index) => "w-[" + Math.ceil((MAX_WIDTH / 7) * index) + "px]");

const ProgressBar = ({ progress }: ProgressProps) => {
  return (
    <>
      <div className="flex justify-start relative">
        <div
          className={MAX_WIDTH_CLASS_NAME + " h-[10px] bg-A706Blue3 rounded-lg"}
        />
        <div
          className={`${widths[progress]} h-[10px] bg-A706CheryBlue rounded-lg absolute`}
        />
      </div>
    </>
  );
};

export default ProgressBar;

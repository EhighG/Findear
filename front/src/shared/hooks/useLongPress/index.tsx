import { useState, useRef, useEffect } from "react";

const LONG_PRESS_DURATION = 500;

const useLongPress = (callback: () => void) => {
  const [longPressTriggered, setLongPressTriggered] = useState(false);
  const timeout = useRef<number>();

  const start = () => {
    timeout.current = setTimeout(() => {
      setLongPressTriggered(true);
      callback();
    }, LONG_PRESS_DURATION);
  };

  const stop = () => {
    if (timeout.current) {
      clearTimeout(timeout.current);
    }
  };

  useEffect(() => {
    return () => {
      stop();
    };
  }, []);

  return {
    onMouseDown: start,
    onMouseUp: stop,
    onMouseLeave: stop,
    onTouchStart: start,
    onTouchEnd: stop,
    longPressTriggered,
  };
};

export default useLongPress;

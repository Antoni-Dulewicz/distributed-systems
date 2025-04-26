import sys, traceback, Ice
import Dynamic

class CalcI(Dynamic.Calc):
    def add(self, a, b, current=None) -> int:
        result = a+b
        print(f"ADD: a = {a}, b = {b}, result = {result}")
        return result

    def subtract(self, a, b, current=None) -> int:
        result = a-b
        print(f"ADD: a = {a}, b = {b}, result = {result}")
        return result

    def op(self, a1, b1, current=None) -> None:
        print("Executing op with structure A:")
        print(f"a: {a1.a}");
        print(f"b: {a1.b}");
        print(f"c: {a1.c}");
        print(f"d: {a1.d}");
        print(f"b1: {b1}");


class Server:
    def run(self, args):
        status = 0
        communicator = None
        try:
            communicator = Ice.initialize(args)
            adapter = communicator.createObjectAdapterWithEndpoints("CalculatorAdapter", "tcp -h 127.0.0.2 -p 10000 -z : udp -h 127.0.0.2 -p 10000 -z");
        
            calc_servant = CalcI()

            identity = Ice.Identity("calc", "calc")
            adapter.add(calc_servant, identity)
            adapter.activate()

            print("Entering event processing loop...")

            communicator.waitForShutdown()

        except:
            traceback.print_exc()
            status = 1
        
        if communicator:
            communicator.destroy()

        sys.exit(status)

if __name__ == "__main__":
    app = Server()
    app.run(sys.argv)

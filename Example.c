int a;
int arr[15];
int fib(int x) {
  int a;
  int b;
  if (x == 1) {
    return 1;
  }
  if (x == 2) {
    return 0;
  }
  a = fib(x - 1);
  b = fib(x - 2);
  return a + b;
}

int foo(int x) {
  return 2;
}

int main(void) {
  int a;
  int b;
  int i;
  int result;
  a = 10;
  b = 20;
  i = 0;

  while (i < 15) {
    arr[i] = i;
    i = i + 1;
  }

  arr[3] = arr[1] + 2 + a + b + $0x66;

  while (a > b) {
    a = 15;
    $0x00 = 1;
    while (a == 1) {
      int c;
      b = b * 2;
      foo(b);
      a = foo(b);
      break;
    }
    if (a > b) {
      b = b + a;
      continue;
    }
    if (a < b) {
      break;
    }
    a = a ^ b;
    a = !a;
    a = a || b;
  }

  result = fib(5);
  
  return result;
}
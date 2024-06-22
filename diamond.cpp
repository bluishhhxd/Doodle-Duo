#include <iostream>
#include <vector>
#include <map>
#include <algorithm>
#include <cmath>
#include <string>
#include <sstream>
#include <numeric>
#include <bits/stdc++.h>
#include <set>
#include <cstdio>
using namespace std;



int main() {
    int n;
    cin>>n;
    system("cls");
    for (int i = 1;i<n+1;i++){
    cout <<string((n-i), ' ') <<string((2*i)-1, '*')<<string((n-i), ' ')<<endl;
    }
    for (int i = 1;i<n+1;i++){
    cout <<string(i-1, ' ') <<string((2*n-2*i)+1, '*')<<string((n-i), ' ')<<endl;
    }    



    return 0;
}

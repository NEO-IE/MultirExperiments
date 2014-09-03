#include<bits/stdc++.h>
using namespace std;
struct node
{
    node(int val,node * a = NULL,node*b= NULL):previous(a),next(b),value(val){}
    ~node(){cout << "value " << value << " is being freed\n";}
    node * previous;
    node * next;
    int value;
} ;

int main()
{

 node * some;
 some = new node(10,some,some);
 cout << some->next->value << endl;

return 0;
}

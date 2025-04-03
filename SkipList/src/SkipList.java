/**
 * 跳表的实现
 */
public class SkipList {
    //跳表最大层级
    static final int MAX_LEVEL=16;
    //跳表当前最大层级
    int CURRENT_LEVEL=0;
    //所有层的头节点
    private final Node h=new Node();

    /**
     * 随机数确定插入数的最大层级
     */
    private int randomLevel(){
        //一定从一开始，因为后续处理的时候是level-1
        int i=1;
        while(Math.random()<0.5f&&i<=MAX_LEVEL){
            i++;
        }
        return i;
    }

    public boolean insert(int value){
        int  level= randomLevel();
        Node newNode=new Node();
        newNode.data=value;
        Node[] maxMine =new Node[level];
        newNode.currentLevel=level;
        //初始化为头节点
        for(int i=0;i<level;i++){
            maxMine[i]=h;
        }
        Node p=h;
        for(int i=level-1;i>=0;i--){
            //重点：不能重置Node=h，不重置这个部分是跳表的关键
            while(p.next[i] != null&&p.next[i].data<value){
                p=p.next[i];
            }
            maxMine[i]=p;
        }
        for(int i=level-1;i>=0;i--){
           newNode.next[i] = maxMine[i].next[i];
           maxMine[i].next[i]=newNode;
        }

        //更新当前跳表的最大层级
        if(CURRENT_LEVEL<level){
            CURRENT_LEVEL=level;
        }
        return true;
    }

    public Node get(int value){
        Node p=h;
        for(int i=CURRENT_LEVEL-1;i>=0;i--){
            while (p.next[i] != null && p.next[i].data < value) {
                p = p.next[i];
            }
            if (p.next[0] != null && p.next[0].data == value) {
                return p.next[0];
            }
        }

        return null;
    }

    public boolean delete(int value){
        Node[] preNode =new Node[CURRENT_LEVEL];
        Node p=h;
        for(int i=CURRENT_LEVEL-1;i>=0;i--){
            while(p.next[i]!=null&&p.next[i].data<value){
                p=p.next[i];
            }
            preNode[i]=p;
        }
        if (p.next[0] != null && p.next[0].data == value) {
            //从最高级索引开始查看其前驱是否等于value，若等于则将当前节点指向value节点的后继节点
            for (int i = CURRENT_LEVEL - 1; i >= 0; i--) {
                if (preNode[i].next[i] != null && preNode[i].next[i].data == value) {
                    preNode[i].next[i] = preNode[i].next[i].next[i];
                }
            }
        }

        while (CURRENT_LEVEL > 1 && h.next[CURRENT_LEVEL-1] == null) {
            CURRENT_LEVEL--;
        }
        return true;
    }




    /**
     * 节点内容
     */
    class Node{
        //节点的最大值，方便后续删除内容
        private int currentLevel=0;
        //节点存储的值
        public int data;
        //下一节点的值
        private Node[] next=new Node[MAX_LEVEL];
        public Node(){}

    }

    public void printAll() {
        for(int i=CURRENT_LEVEL-1; i>-1; i--) {
            Node p = h;
            //基于最底层的非索引层进行遍历，只要后继节点不为空，则速速出当前节点，并移动到后继节点
            while (p.next[i] != null) {
                System.out.print(p.next[i].data+"->");
                p = p.next[i];
            }
            System.out.println();
        }


    }



    public static void main(String[] args) {
        SkipList skipList = new SkipList();
        for (int i = 0; i < 24; i++) {
            skipList.insert(i);
        }
        skipList.printAll();
        skipList.insert(27);
        skipList.insert(28);
        skipList.delete(27);
        System.out.println("**********输出添加结果**********");
        skipList.printAll();

        SkipList.Node node = skipList.get(22);
        System.out.println("**********查询结果:" + node.data+" **********");

        skipList.insert(26);

        skipList.delete(0);
        System.out.println("**********删除结果**********");
        skipList.printAll();


    }
}

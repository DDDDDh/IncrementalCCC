%读取文件输入，这里采用绝对路径
T=readtable("/Users/yi-huang/Project/IncrementalCCC/target/Data/Running0808/testExcel.xlsx")
%注意变量的选择，要对应表格中相应的列
opNum=table2array(T(:,1));
processNum=table2array(T(:,2));
bco_avg=table2array(T(:,5));
ico_avg=table2array(T(:,4));
xmax=max(opNum);
xmin=min(opNum);
ymax=max(processNum);
ymin=min(processNum);
[X,Y]=meshgrid(xmin:100:xmax,ymin:5:ymax);
Z=griddata(opNum,processNum,bco_avg,X,Y);
I=griddata(opNum,processNum,ico_avg,X,Y);
bco = mesh(X,Y,Z,'FaceAlpha','0','EdgeColor','blue');
bco.LineStyle='-';
hold on
ico=mesh(X,Y,I,'FaceAlpha','0','EdgeColor','red');
ico.LineStyle='--';
xlabel('opNum');
ylabel('processNum');
zlabel('Time(ns)');
legend('bco','ico')
%输出到指定文件
savefig("/Users/yi-huang/Project/IncrementalCCC/target/Data/Running0808/CoCompare.fig");
saveas(gcf,"/Users/yi-huang/Project/IncrementalCCC/target/Data/Running0808/CoCompare.jpg");
package restAPIAccess;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.eclipse.jdt.core.dom.ASTVisitor;


import Node.IndexHits;
import Node.NodeJSON;
import RestAPI.GraphServerAccess;
import RestAPI.ThreadedMethodContainerFetch;

import com.google.common.collect.HashMultimap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.json.JSONArray;
import org.json.JSONObject;

class SubsequentASTVisitor extends ASTVisitor
{
	public HashMap<NodeJSON, NodeJSON> methodContainerCache;
	public HashMap<NodeJSON, NodeJSON> methodReturnCache;
	public HashMap<String, ArrayList<NodeJSON>> candidateClassNodesCache;
	public HashMap<String, ArrayList<NodeJSON>> candidateMethodNodesCache;
	public HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache;
	public HashMap<String, ArrayList<NodeJSON>> parentNodeCache;
	private HashMap<String, ArrayList<ArrayList<NodeJSON>>> shortClassShortMethodCache;
	private HashMap<String, IndexHits<NodeJSON>> allMethodsInClass; //FQ classId -> list of methods
	
	private int NThreads = 10;
	public GraphServerAccess model;
	public CompilationUnit cu;
	public int cutype;
	public HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>> methodReturnTypesMap;
	public HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>> variableTypeMap;//holds variables, fields and method param types
	public HashMultimap<Integer, NodeJSON> printtypes;//holds node start loc and possible types
	public HashMultimap<Integer, NodeJSON> printmethods;//holds node start posns and possible methods they can be
	public HashMap<String, Integer> printTypesMap;//maps node start loc to variable names
	public HashMap<String, Integer> printMethodsMap;//holds node start locs with method names
	public Set<String> importList;
	public Stack<String> classNames;
	public String superclassname;
	public ArrayList<Object> interfaces;
	public int tolerance;
	public int MAX_CARDINALITY;
	private HashMultimap<String, String> localMethods;
	private HashSet<String> localClasses;
	private JSONObject json;
	public HashSet<String> primitiveTypesSet;
	
	public void printFields()
	{
		System.out.println("methodReturnTypesMap: " + methodReturnTypesMap);
		System.out.println("variableTypeMap: " + variableTypeMap);
		System.out.println("printtypes: " + printtypes);
		System.out.println("printmethods: " + printmethods);
		System.out.println("printTypesMap: " + printTypesMap);
		System.out.println("printMethodsMap: " + printMethodsMap);
		System.out.println("possibleImportList: " + importList);
		System.out.println("localMethods: " + localMethods);
	}

	public SubsequentASTVisitor(FirstASTVisitor previousVisitor) 
	{
		
		model = previousVisitor.model;
		cu = previousVisitor.cu;
		cutype = previousVisitor.cutype;
		variableTypeMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>(previousVisitor.variableTypeMap);
		methodReturnTypesMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>(previousVisitor.methodReturnTypesMap);
		printtypes = HashMultimap.create(previousVisitor.printtypes);
		printmethods = HashMultimap.create(previousVisitor.printmethods);
		printTypesMap = new HashMap<String, Integer>(previousVisitor.printTypesMap);
		printMethodsMap = new HashMap<String, Integer>(previousVisitor.printMethodsMap);
		importList = new HashSet<String>(previousVisitor.importList);
		classNames = previousVisitor.classNames;
		superclassname = new String(previousVisitor.superclassname);
		interfaces = new ArrayList<Object>(previousVisitor.interfaces);
		methodContainerCache = new HashMap<NodeJSON, NodeJSON>(previousVisitor.methodContainerCache);
		methodReturnCache = new HashMap<NodeJSON, NodeJSON>(previousVisitor.methodReturnCache);
		parentNodeCache = new HashMap<String, ArrayList<NodeJSON>>(previousVisitor.parentNodeCache);
		allMethodsInClass = new HashMap<String, IndexHits<NodeJSON>>(previousVisitor.allMethodsInClass);
		tolerance = previousVisitor.tolerance;
		MAX_CARDINALITY = previousVisitor.MAX_CARDINALITY;
		localMethods = HashMultimap.create(previousVisitor.localMethods);
		localClasses = new HashSet<String>(previousVisitor.localClasses);
		shortClassShortMethodCache = new HashMap<String, ArrayList<ArrayList<NodeJSON>>>(previousVisitor.shortClassShortMethodCache);
		primitiveTypesSet = previousVisitor.primitiveTypesSet;
		//removeClustersIfAny();
		updateImports();
		updateBasedOnImports();
	}
	
	public SubsequentASTVisitor(SubsequentASTVisitor previousVisitor) 
	{
		model = previousVisitor.model;
		cu = previousVisitor.cu;
		cutype = previousVisitor.cutype;
		variableTypeMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>(previousVisitor.variableTypeMap);
		methodReturnTypesMap = new HashMap<String, HashMultimap<ArrayList<Integer>,NodeJSON>>(previousVisitor.methodReturnTypesMap);
		printtypes = HashMultimap.create(previousVisitor.printtypes);
		printmethods = HashMultimap.create(previousVisitor.printmethods);
		printTypesMap = new HashMap<String, Integer>(previousVisitor.printTypesMap);
		printMethodsMap = new HashMap<String, Integer>(previousVisitor.printMethodsMap);
		importList = new HashSet<String>(previousVisitor.importList);
		classNames = previousVisitor.classNames;
		superclassname = new String(previousVisitor.superclassname);
		interfaces = new ArrayList<Object>(previousVisitor.interfaces);
		methodContainerCache = new HashMap<NodeJSON, NodeJSON>(previousVisitor.methodContainerCache);
		methodReturnCache = new HashMap<NodeJSON, NodeJSON>(previousVisitor.methodReturnCache);
		parentNodeCache = new HashMap<String, ArrayList<NodeJSON>>(previousVisitor.parentNodeCache);
		shortClassShortMethodCache = new HashMap<String, ArrayList<ArrayList<NodeJSON>>>(previousVisitor.shortClassShortMethodCache);
		allMethodsInClass = new HashMap<String, IndexHits<NodeJSON>>(previousVisitor.allMethodsInClass);
		tolerance = previousVisitor.tolerance;
		MAX_CARDINALITY = previousVisitor.MAX_CARDINALITY;
		localMethods = HashMultimap.create(previousVisitor.localMethods);
		localClasses = new HashSet<String>(previousVisitor.localClasses);
		primitiveTypesSet = previousVisitor.primitiveTypesSet;
		removeClustersIfAny();
		updateImports();
		updateBasedOnImports();
	}
	
	public void updateImports()
	{
		for(Integer key : printtypes.keySet())
		{
			Set<NodeJSON> temp = printtypes.get(key);
			if(temp.size() < tolerance)
			{
				for(NodeJSON node : temp)
					addCorrespondingImport((String)node.getProperty("id"));
			}
		}
		
		for(Integer key : printmethods.keySet())
		{
			Set<NodeJSON> temp = printmethods.get(key);
			List<NodeJSON> parentNodes = Collections.synchronizedList(new ArrayList<NodeJSON>());
			
			if(temp.size() < tolerance)
			{
				ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
				for(NodeJSON node : temp)
				{
					ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(node, methodContainerCache, parentNodes, model);
					getMethodContainerExecutor.execute(tmcf);
				}
				getMethodContainerExecutor.shutdown();
				while(getMethodContainerExecutor.isTerminated() == false)
				{
					
				}
			
				for(NodeJSON parent : parentNodes)
					importList.add(parent.getProperty("id"));
			}
		}
	}
	
	private void addCorrespondingImport(String classID)
	{
		int loc = classID.indexOf('.');
		if(loc != -1)
		{
			String possibleImport = classID.substring(0, classID.lastIndexOf(".")) + ".*";
			importList.add(possibleImport);
			//importList.add(classID);
		}
	}
	
	public void updateBasedOnImports()
	{
		//Update variableTypeMap to hold only a possible import if one exists. Else leave untouched.
		HashMultimap<Integer, NodeJSON> tempprinttypes = HashMultimap.create();
		for(Integer key : printtypes.keySet())
		{
			Set<NodeJSON> list = printtypes.get(key);
			Set<NodeJSON> newList = getNewClassElementsList(list);
			if(!newList.isEmpty())
			{
				tempprinttypes.putAll(key, newList);
			}
			else
			{
				tempprinttypes.putAll(key, list);
			}
		}
		printtypes = tempprinttypes;
		
		
		HashMultimap<Integer, NodeJSON> tempprintmethods = HashMultimap.create();
		for(Integer key : printmethods.keySet())
		{
			Set<NodeJSON> list = printmethods.get(key);
			HashSet<NodeJSON> temp = new HashSet<NodeJSON>();
			for(NodeJSON method : list)
			{
				NodeJSON conatiner = model.getMethodContainer(method, methodContainerCache);
				temp.add(conatiner);
			}
			
			Set<NodeJSON> replacementList = getNewClassElementsList(temp);
			
			HashSet<NodeJSON> newList = new HashSet<NodeJSON>();
			for(NodeJSON method : list)
			{
				if( contains(replacementList, model.getMethodContainer(method, methodContainerCache)) )
				{
					newList.add(method);
				}
			}
			
			if(!newList.isEmpty())
			{
				tempprintmethods.putAll(key, newList);
			}
			else
			{
				tempprintmethods.putAll(key, list);
			}
		}
		printmethods = tempprintmethods;
	}
	
	private Set<NodeJSON> getNewClassElementsList(Set<NodeJSON> candidateClassElements)
	{
		HashSet<String> completeSet = new HashSet<String>();
		HashSet<String> prunedSet = new HashSet<String>();
		Set<NodeJSON> classElementsArrayList = new HashSet<NodeJSON>();
		Set<NodeJSON> prunedClassElementList = new HashSet<NodeJSON>();
		int flag = 0;

		for(NodeJSON classElement: candidateClassElements)
		{
			if (classElement == null)//added by nmeng
				continue;
			String className = (String) classElement.getProperty("id");
			if(!completeSet.contains(className))
			{
				classElementsArrayList.add(classElement);
				completeSet.add(className);
			}
			if(!importList.isEmpty())
			{
				for(String importItem : importList)
				{
					if(importItem.contains(".*"))
					{
						importItem = importItem.substring(0, importItem.indexOf(".*"));
					}
					if(className.startsWith(importItem))
					{
						if(!prunedSet.contains(className))
						{
							prunedSet.add(className);
							prunedClassElementList.add(classElement);
							flag = 1;
						}
					}
				}
			}
			if(className.startsWith("java.lang"))
			{
				if(!prunedSet.contains(className))
				{
					prunedSet.add(className);
					prunedClassElementList.add(classElement);
					flag = 1;
				}
			}
		}
		if(flag == 0)
			return classElementsArrayList;
		else
			return prunedClassElementList;
	}

	private ArrayList<Integer> getScopeArray(ASTNode treeNode)
	{
		ASTNode parentNode;
		ArrayList<Integer> parentList = new ArrayList<Integer>();
		while((parentNode =treeNode.getParent())!=null)
		{
			parentList.add(parentNode.getStartPosition());
			treeNode = parentNode;
		}
		return parentList;
	}

	public boolean isLocalMethod(String methodName)
	{
		return false;
	}

	
	public void endVisit(VariableDeclarationFragment treeNode)
	{
		/*
		String vname = treeNode.getName().toString();
		int startPosition = treeNode.getParent().getStartPosition();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap = variableTypeMap.get(vname);
		if(temporaryMap == null)
			return;
		Set<NodeJSON> candidateClassNodes;
		ArrayList<Integer> rightScopeArray1 = getNodeSet(temporaryMap, scopeArray);
		if(rightScopeArray1 == null)
		{
			candidateClassNodes = new HashSet<NodeJSON>();
		}
		else
		{
			candidateClassNodes = temporaryMap.get(rightScopeArray1);
		}
		candidateClassNodes = getNewClassElementsList(candidateClassNodes);
		Set<NodeJSON> currentTypes = printtypes.get(startPosition);
		System.out.println("yeah " + vname + currentTypes.size());
		Set<NodeJSON> newClassNodes = new HashSet<NodeJSON>();
		for(NodeJSON type : currentTypes)
		{
			if(contains(candidateClassNodes,type) == true)
			{
				newClassNodes.add(type);
			}
		}
		
		if(newClassNodes.size() < tolerance)
		{
			for(NodeJSON newClassNode : newClassNodes)
			{
				String possibleImport = getCorrespondingImport(newClassNode.getProperty("id").toString());
				if(possibleImport!=null)
				{
					importList.add(possibleImport);
				}
			}
		}
		
		temporaryMap.replaceValues(rightScopeArray1, newClassNodes);
		printtypes.putAll(startPosition, newClassNodes);
		 */
	}
	
	public void endVisit(MethodInvocation treeNode)
	{
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		Expression expression = treeNode.getExpression();
		String treeNodeString = treeNode.toString();
		int startPosition = treeNode.getName().getStartPosition();
		if(expression == null)
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap2 = methodReturnTypesMap.get(treeNodeString);
			if(temporaryMap2 == null)
				return;

			ArrayList<Integer> rightScopeArray2 = getNodeSet(temporaryMap2, scopeArray);
			if(rightScopeArray2 == null)
				return;
			Set<NodeJSON> candidateReturnNodes = temporaryMap2.get(rightScopeArray2);
			Set<NodeJSON> currentMethods = printmethods.get(startPosition);
			
			Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
			Set<NodeJSON> newReturnNodes = new HashSet<NodeJSON>();
			for(NodeJSON method : currentMethods)
			{
				NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
				if(contains(candidateReturnNodes, returnNode) == true)
				{
					newMethodNodes.add(method);
					newReturnNodes.add(returnNode);
				}
			}
			printmethods.removeAll(startPosition);
			printmethods.putAll(startPosition, newMethodNodes);
			temporaryMap2.removeAll(rightScopeArray2);
			temporaryMap2.putAll(rightScopeArray2, newReturnNodes);
		}
		else if(expression.toString().contains("System."))
		{
			
		}
		else if(expression.getNodeType() == 2)
		{
		}
		else if(variableTypeMap.containsKey(expression.toString()))
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap1 = variableTypeMap.get(expression.toString());
			if(temporaryMap1 == null)
				return;
			Set<NodeJSON> candidateClassNodes;
			ArrayList<Integer> rightScopeArray1 = getNodeSet(temporaryMap1, scopeArray);
			if(rightScopeArray1 == null)
			{
				candidateClassNodes = new HashSet<NodeJSON>();
			}
			else
			{
				candidateClassNodes = temporaryMap1.get(rightScopeArray1);
			}
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap2 = methodReturnTypesMap.get(treeNodeString);
			if(temporaryMap2 == null)
				return;
			ArrayList<Integer> rightScopeArray2 = getNodeSet(temporaryMap2, scopeArray);
			Set<NodeJSON> candidateReturnNodes;
			if(rightScopeArray2 == null)
			{
				candidateReturnNodes = new HashSet<NodeJSON>();
			}
			else
			{
				candidateReturnNodes = temporaryMap2.get(rightScopeArray2);
			}
			Set<NodeJSON> currentMethods = printmethods.get(startPosition);
			Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
			Set<NodeJSON> newReturnNodes = new HashSet<NodeJSON>();
			Set<NodeJSON> newClassNodes = new HashSet<NodeJSON>();
			int foundFlag= 0;
			
			if(currentMethods.size() == 0)
				return;
			for(NodeJSON candidateClassNode : candidateClassNodes)
			{
				for(NodeJSON method : currentMethods)
				{
					NodeJSON parentNode = model.getMethodContainer(method, methodContainerCache);
					NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
					
					if(parentNode.getProperty("id").equals(candidateClassNode.getProperty("id")) && contains(candidateReturnNodes, returnNode))
					{
						newMethodNodes.add(method);
						newClassNodes.add(parentNode);
						newReturnNodes.add(returnNode);
						foundFlag = 1;
					}
				}
			}
			
			
			if(foundFlag == 0)
			{
				for(NodeJSON candidateClassNode : candidateClassNodes)
				{
					for(NodeJSON method : currentMethods)
					{
						NodeJSON parentNode = model.getMethodContainer(method, methodContainerCache);
						NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
						
						if(model.checkIfParentNode(parentNode, candidateClassNode.getProperty("id"), parentNodeCache)  && contains(candidateReturnNodes, returnNode))
						{
							newMethodNodes.add(method);
							newClassNodes.add(parentNode);
							newReturnNodes.add(returnNode);
							foundFlag = 1;
						}
					}
				}
			}
			
			if(newClassNodes.size() < tolerance)
			{
				for(NodeJSON newClassNode : newClassNodes)
					addCorrespondingImport(newClassNode.getProperty("id").toString());
			}
			temporaryMap1.replaceValues(rightScopeArray1, newClassNodes);
			variableTypeMap.put(expression.toString(), temporaryMap1);
			printtypes.replaceValues(printTypesMap.get(expression.toString()), newClassNodes);
			printtypes.replaceValues(startPosition, newClassNodes);
			temporaryMap2.replaceValues(rightScopeArray2, newReturnNodes);
			methodReturnTypesMap.put(treeNodeString, temporaryMap2);
			printmethods.replaceValues(startPosition, newMethodNodes);
		}
		else if(methodReturnTypesMap.containsKey(expression.toString()))
		{
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap1 = methodReturnTypesMap.get(expression.toString());
			if(temporaryMap1 == null)
				return;
			ArrayList<Integer> rightScopeArray1 = getNodeSet(temporaryMap1, scopeArray);
			if(rightScopeArray1 == null)
				return;
			Set<NodeJSON> candidateClassNodes = temporaryMap1.get(rightScopeArray1);
			candidateClassNodes = getNewClassElementsList(candidateClassNodes);
			
			HashMultimap<ArrayList<Integer>, NodeJSON> temporaryMap2 = methodReturnTypesMap.get(treeNodeString);
			if(temporaryMap2 == null)
				return;
			ArrayList<Integer> rightScopeArray2 = getNodeSet(temporaryMap2, scopeArray);
			if(rightScopeArray2 == null)
				return;
			Set<NodeJSON> candidateReturnNodes = temporaryMap2.get(rightScopeArray2);
			Set<NodeJSON> currentMethods = printmethods.get(startPosition);
			Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
			Set<NodeJSON> newReturnNodes = new HashSet<NodeJSON>();
			Set<NodeJSON> newClassNodes = new HashSet<NodeJSON>();
			
			for(NodeJSON method : currentMethods)
			{
				NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
				NodeJSON parentNode = model.getMethodContainer(method, methodContainerCache);
				if(contains(candidateClassNodes, parentNode) == true && contains(candidateReturnNodes, returnNode) == true)
				{
					newMethodNodes.add(method);
					newReturnNodes.add(returnNode);
					newClassNodes.add(parentNode);
				}
			}
			if(newClassNodes.size() < tolerance)
			{
				for(NodeJSON newClassNode : newClassNodes)
					addCorrespondingImport(newClassNode.getProperty("id").toString());
			}
			temporaryMap1.removeAll(rightScopeArray1);
			temporaryMap1.putAll(rightScopeArray1, newClassNodes);
			printmethods.removeAll(startPosition);
			printmethods.putAll(startPosition, newMethodNodes);
			temporaryMap2.removeAll(rightScopeArray2);
			temporaryMap2.putAll(rightScopeArray2, newReturnNodes);
		}
	}

	private boolean contains(Set<NodeJSON> candidateNodes, NodeJSON containerNode) 
	{
		String id = containerNode.getProperty("id");
		for(NodeJSON node : candidateNodes)
		{
			if(node.getProperty("id").equals(id))
				return true;
		}
		return false;
	}

	private ArrayList<Integer> getNodeSet(HashMultimap<ArrayList<Integer>, NodeJSON> celist2, ArrayList<Integer> scopeArray) 
	{
		for(ArrayList<Integer> test : celist2.keySet())
		{
			if(isSubset(test, scopeArray))
				return test;
		}
		return null;
	}

	private boolean isSubset(ArrayList<Integer> test,ArrayList<Integer> scopeArray) 
	{
		if(scopeArray.containsAll(test))
			return true;
		/*else if(scopeArray.containsAll(test.subList(1, test.size())))
			return true;*/
		else
			return false;
	}

	public void endVisit(ConstructorInvocation treeNode)
	{
		/*
		String treeNodeString = treeNode.toString();
		int startPosition = treeNode.getStartPosition();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		
		Set<NodeJSON> candidateReturnNodes = methodReturnTypesMap.get(treeNodeString).get(scopeArray);
		
		Set<NodeJSON> currentMethods = printmethods.get(startPosition);
		
		Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
		
		for(NodeJSON method : currentMethods)
		{
			NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
			if(contains(candidateReturnNodes, returnNode) == true)
			{
				newMethodNodes.add(method);
			}
		}
		printmethods.removeAll(startPosition);
		printmethods.putAll(startPosition, newMethodNodes);
		 */
	}
	
	public void endVisit(ClassInstanceCreation treeNode)
	{
		if(treeNode.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT)
		{
			int startPosition = treeNode.getType().getStartPosition();
			
			VariableDeclarationFragment lhs = ((VariableDeclarationFragment) treeNode.getParent());
			
			HashMultimap<ArrayList<Integer>, NodeJSON> tempMap; 
			Set<NodeJSON> candidateClassNodes = new HashSet<NodeJSON>();
			
			tempMap = variableTypeMap.get(lhs.getName().toString());
			if(tempMap != null)
			{
				if(!tempMap.keySet().isEmpty())
				{
					ArrayList<Integer> scopeArray = getScopeArray(treeNode.getParent());
					ArrayList<Integer> rightScopeArray = getNodeSet(tempMap, scopeArray);
					candidateClassNodes = tempMap.get(rightScopeArray);
				}
			}
			
			
			tempMap.putAll(getScopeArray(lhs.getParent()), printtypes.get(startPosition));
			Set<NodeJSON> currentClassNodes = printtypes.get(startPosition);
			Set<NodeJSON> currentMethodNodes = printmethods.get(startPosition);
			currentClassNodes = getNewClassElementsList(currentClassNodes);
			Set<NodeJSON> newClassNodes = new HashSet<NodeJSON>();
			
			for(NodeJSON type : currentClassNodes)
			{
				if(contains(candidateClassNodes,type) == true)
				{
					newClassNodes.add(type);
				}
			}
			
			if(newClassNodes.size() < tolerance)
			{
				for(NodeJSON newClassNode : newClassNodes)
					addCorrespondingImport(newClassNode.getProperty("id").toString());
			}
			
			Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
			for(NodeJSON method : currentMethodNodes)
			{
				NodeJSON parent = model.getMethodContainer(method, methodContainerCache);
				if(contains(candidateClassNodes, parent))
				{
					newMethodNodes.add(method);
				}
			}
			
			printtypes.replaceValues(startPosition, newClassNodes);
			printmethods.replaceValues(startPosition, newMethodNodes);
		}
	}

	public void endVisit(SuperConstructorInvocation treeNode)
	{	
		String treeNodeString = treeNode.toString();
		int startPosition = treeNode.getStartPosition();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		ArrayList<NodeJSON> newContainerSet = new ArrayList<NodeJSON>();
		Set<NodeJSON> candidateReturnNodes = methodReturnTypesMap.get(treeNodeString).get(scopeArray);
		
		Set<NodeJSON> currentMethods = printmethods.get(startPosition);
		
		ArrayList<NodeJSON> newMethodNodes = new ArrayList<NodeJSON>();
		for(NodeJSON method : currentMethods)
		{
			//since its a constructor, return is the container class
			//NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
			NodeJSON returnNode = model.getMethodContainer(method, methodContainerCache);
			if(contains(candidateReturnNodes, returnNode) == true)
			{
				newContainerSet.add(model.getMethodContainer(method, methodContainerCache));
				newMethodNodes.add(method);
			}
		}
		printmethods.replaceValues(startPosition, newMethodNodes);
		printtypes.replaceValues(startPosition, newContainerSet);
	}

	public void endVisit(SuperMethodInvocation treeNode)
	{
		String treeNodeString = treeNode.toString();
		int startPosition = treeNode.getStartPosition();
		ArrayList<Integer> scopeArray = getScopeArray(treeNode);
		if(methodReturnTypesMap.containsKey(treeNodeString))
		{
			Set<NodeJSON> candidateReturnNodes = methodReturnTypesMap.get(treeNodeString).get(scopeArray);
			
			Set<NodeJSON> currentMethods = printmethods.get(startPosition);
			
			Set<NodeJSON> newMethodNodes = new HashSet<NodeJSON>();
			
			for(NodeJSON method : currentMethods)
			{
				NodeJSON returnNode = model.getMethodReturn(method, methodReturnCache);
				if(contains(candidateReturnNodes, returnNode) == true)
				{
					newMethodNodes.add(method);
				}
			}
			printmethods.removeAll(startPosition);
			printmethods.putAll(startPosition, newMethodNodes);
		}
	}

	public void removeClustersIfAny()
	{
		HashMultimap<Integer, NodeJSON> tempprinttypes = HashMultimap.create();
		HashMultimap<Integer, NodeJSON> tempprintmethods = HashMultimap.create();
		for(Integer key : printtypes.keySet())
		{
			NodeJSON node = model.returnRightNodeIfCluster(printtypes.get(key));
			if(node != null)
			{
				tempprinttypes.put(key, node);
			}
			else
				tempprinttypes.putAll(key, printtypes.get(key));
		}
		for(Integer key : printmethods.keySet())
		{
			Set<NodeJSON> parentSet = new HashSet<NodeJSON>();
			for(NodeJSON method : printmethods.get(key))
			{
				NodeJSON parent = model.getMethodContainer(method, methodContainerCache);
				parentSet.add(parent);
			}
			NodeJSON node = model.returnRightNodeIfCluster(parentSet);
			if(node != null)
			{
				for(NodeJSON method : printmethods.get(key))
				{
					NodeJSON parent = model.getMethodContainer(method, methodContainerCache);
					if(parent.equals(node))
					{
						tempprintmethods.put(key, method);
					}
				}
				if(!tempprintmethods.containsKey(key))
					tempprintmethods.putAll(key, printmethods.get(key));
			}
			else
			{
				tempprintmethods.putAll(key, printmethods.get(key));
			}
		}
		
		printtypes = tempprinttypes;
		printmethods = tempprintmethods;
		
	}
	
	private boolean isPrimitive(String arg) 
	{
		if(primitiveTypesSet.contains(arg))
			return true;
		return false;
	}
	
	public void setJson()
	{
		checkForNull();
		
		JSONObject main_json=new JSONObject();
		
		for(Integer key : printtypes.keySet())
		{
			int flag = 0;
			String cname = null;
			ArrayList<String> namelist = new ArrayList<String>();
			if(printtypes.get(key).size() < MAX_CARDINALITY)
			{
				Set<NodeJSON> prunedValueSet = removeInheritedRetainParentType(printtypes.get(key));
				for(NodeJSON type_name:prunedValueSet)
				{
					//if(!isPrimitive(type_name.getProperty("id")))
					{
						String nameOfClass = (String)type_name.getProperty("id");
						nameOfClass = JSONObject.quote(nameOfClass);
						namelist.add("\""+nameOfClass+"\"");
						if(flag == 0)
						{
							cname = (String) type_name.getProperty("exactName");
							flag = 1;
						}
					}
	
				}
				if(namelist.isEmpty() == false)
				{
					JSONObject json = new JSONObject();
					json.accumulate("line_number",Integer.toString(cu.getLineNumber(key)-cutype));
					json.accumulate("precision", Integer.toString(namelist.size()));
					json.accumulate("name",cname);
					json.accumulate("elements",namelist);
					json.accumulate("type","api_type");
					json.accumulate("character", Integer.toString(key));
					main_json.accumulate("api_elements", json);
				}
				
			}
		}
		
		for(Integer key : printmethods.keySet())
		{
			ArrayList<String> namelist = new ArrayList<String>();
			String mname = null;
			if(printmethods.get(key).size() < MAX_CARDINALITY)
			{
				Set<NodeJSON> prunedValueSet = removeInheritedRetainParentMethod(printmethods.get(key));
				for(NodeJSON method_name : prunedValueSet)
				{
					String nameOfMethod = (String)method_name.getProperty("id");
					nameOfMethod = JSONObject.quote(nameOfMethod);
					namelist.add("\""+nameOfMethod+"\"");
					mname=(String) method_name.getProperty("exactName");
				}
				
				
				if(namelist.isEmpty() == false)
				{
					JSONObject json = new JSONObject();
					json.accumulate("line_number",Integer.toString(cu.getLineNumber(key)-cutype));
					json.accumulate("precision", Integer.toString(namelist.size()));
					json.accumulate("name",mname);
					json.accumulate("elements",namelist);
					json.accumulate("type","api_method");
					json.accumulate("character", Integer.toString(key));
					main_json.accumulate("api_elements", json);
				}
			}
		}
		if(main_json.isNull("api_elements"))
		{
			String emptyJSON = "{\"api_elements\": []}" ;
			JSONObject ret = new JSONObject();
			try 
			{
				ret = new JSONObject(emptyJSON);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			this.json = ret;
		}
		else
		{
			this.json = main_json;
		}
	}
	
	
	private Set<NodeJSON> removeInheritedRetainParentMethod(Set<NodeJSON> set) 
	{
		Collection<NodeJSON> removeSet = new ArrayList<NodeJSON>();
		for(NodeJSON parent : set)
		{
			NodeJSON pclass = model.getMethodContainer(parent, methodContainerCache);
			for(NodeJSON child : set)
			{
				NodeJSON cclass = model.getMethodContainer(child, methodContainerCache);
				if(!pclass.equals(cclass))
				{
					if(model.checkIfParentNode(pclass, (String)cclass.getProperty("id"), parentNodeCache))
						removeSet.add(child);
				}
			}
		}
		set.removeAll(removeSet);
		return set;
	}

	
	private Set<NodeJSON> removeInheritedRetainParentType(Set<NodeJSON> set) 
	{
		Collection<NodeJSON> removeSet = new ArrayList<NodeJSON>();
		for(NodeJSON parent : set)
		{
			for(NodeJSON child : set)
			{
				if(!parent.equals(child))
				{
					if(model.checkIfParentNode(parent, (String)child.getProperty("id"), parentNodeCache))
						removeSet.add(child);
				}
			}
		}
		set.removeAll(removeSet);
		return set;
	}
	
	public JSONObject getJson()
	{
		if (this.json.get("api_elements") instanceof JSONObject)
		{
			return sortJSON(this.json);
		}
		else if (this.json.get("api_elements") instanceof JSONArray)
		{
			JSONArray elements = (JSONArray) this.json.get("api_elements");
			if(elements.length() == 0)
			{
				return this.json;
			}
			else
			{
				return sortJSON(this.json);
			}
		}
		else
			return this.json;
	}

	private JSONObject sortJSON(JSONObject json) 
	{
		Comparator<JSONObject> comprator = new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2)
			{
				if(o1.getInt("character") != o2.getInt("character"))
					return(o1.getInt("character") - o2.getInt("character"));
				else
				{
					return 1;
				}
			}
		};
		TreeSet<JSONObject> set = new TreeSet<JSONObject>(comprator);
		if(json.get("api_elements") instanceof JSONArray)
		{
			JSONArray array = (JSONArray) json.get("api_elements");
			for(int i=0; i<array.length(); i++)
			{
				JSONObject entry = (JSONObject) array.get(i);
				set.add(entry);
			}
		}
		else if(json.get("api_elements") instanceof JSONObject)
		{
			JSONObject array = (JSONObject) json.get("api_elements");
			set.add(array);
		}
		JSONObject newObj = new JSONObject();
		for(JSONObject obj : set)
		{
			newObj.accumulate("api_elements", obj);
		}
		
		return newObj;
	}

	public void checkForNull()
	{
		printtypes.removeAll(null);
		printmethods.removeAll(null);
		for(Integer key : printtypes.keySet())
		{
			for(NodeJSON type_name : printtypes.get(key))
			{
				if(type_name==null)
					printtypes.remove(key, type_name);
			}
		}
		
		for(Integer key : printmethods.keySet())
		{
			for(NodeJSON method_name : printmethods.get(key))
			{
				if(method_name==null)
					printmethods.remove(key, method_name);
			}
		}
	}
}
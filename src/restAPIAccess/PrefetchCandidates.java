package restAPIAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.eclipse.jdt.core.dom.ASTVisitor;


import Node.IndexHits;
import Node.NodeJSON;
import RestAPI.GraphServerAccess;
import RestAPI.ThreadedClassFetch;
import RestAPI.ThreadedMethodFetch;

import com.google.common.collect.HashMultimap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

class PrefetchCandidates extends ASTVisitor
{
	boolean flag = true;
	int NThreads = 20;
	ExecutorService classFetchExecutor = Executors.newFixedThreadPool(NThreads);
	ExecutorService methodFetchExecutor = Executors.newFixedThreadPool(NThreads);
	public GraphServerAccess model;
	public CompilationUnit cu;
	public int cutype;
	public HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache;
	public HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache;
	public HashMap<NodeJSON, NodeJSON> methodContainerCache;
	public HashMap<NodeJSON, NodeJSON> methodReturnCache;
	public HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache;
	public HashMap<String, ArrayList<NodeJSON>> parentNodeCache;
	public HashMap<String, ArrayList<ArrayList<NodeJSON>>> shortClassShortMethodCache;
	public HashMap<String, IndexHits<NodeJSON>> allMethodsInClass;
	
	public HashSet<String> processedClasses;

	public HashMultimap<String, String> localMethods; 	//list of localMethods mapped to their corresponding parent classes
	public HashSet<String> localClasses;				//List of local class names
	private HashSet<String> processedMethods;
	
	public Stack<String> classNames; 
	public String superclassname;
	public ArrayList<String> interfaces;
	public int tolerance;
	public int MAX_CARDINALITY;

	public HashSet<String> primitiveTypesSet;
	
	public PrefetchCandidates(GraphServerAccess db, CompilationUnit cu, int cutype, int tolerance, int max_cardinality) 
	{
		this.model = db;
		this.cu = cu;
		this.cutype = cutype;
		this.tolerance = tolerance;
		MAX_CARDINALITY = max_cardinality;
		initializeFields();
		fetchLocalClassesAndMethods(cu);
	}

	/*
	 * Initialize fields in the PrefetchCandidate class
	 */
	private void initializeFields()
	{
		localMethods = HashMultimap.create();
		localClasses = new HashSet<String>();

		candidateClassNodesCache = new HashMap<String, IndexHits<NodeJSON>>();
		candidateMethodNodesCache = new HashMap<String, IndexHits<NodeJSON>>();
		methodContainerCache = new HashMap<NodeJSON, NodeJSON>();
		methodReturnCache = new HashMap<NodeJSON, NodeJSON>();
		methodParameterCache = new HashMap<NodeJSON, ArrayList<NodeJSON>>();
		parentNodeCache = new HashMap<String, ArrayList<NodeJSON>>();
		shortClassShortMethodCache = new HashMap<String, ArrayList<ArrayList<NodeJSON>>>();
		allMethodsInClass = new HashMap<String, IndexHits<NodeJSON>>();
		
		classNames = new Stack<String>();
		superclassname = new String();
		interfaces = new ArrayList<String>();

		processedClasses = new HashSet<String>();
		processedMethods = new HashSet<String>();
		
		primitiveTypesSet = new HashSet<String>();
		primitiveTypesSet.add("int");
		primitiveTypesSet.add("long");
		primitiveTypesSet.add("float");
		primitiveTypesSet.add("char");
		primitiveTypesSet.add("byte");
		primitiveTypesSet.add("boolean");
		primitiveTypesSet.add("byte[]");
		primitiveTypesSet.add("int[]");
		primitiveTypesSet.add("float[]");
		primitiveTypesSet.add("char[]");
		primitiveTypesSet.add("long[]");
		primitiveTypesSet.add("boolean[]");
	}


	/*
	 * Populates a list of local classes and methods inside these classes into the localClasses and localMethods fields.
	 */
	private void fetchLocalClassesAndMethods(CompilationUnit cu) 
	{
		cu.accept(new ASTVisitor() {
			Stack<String> classNames = new Stack<String>(); 
			public boolean visit(TypeDeclaration treeNode)
			{
				String currentClassName = treeNode.getName().toString(); 
				classNames.push(currentClassName);
				localClasses.add(currentClassName);
				return true;
			}

			public boolean visit(final ClassInstanceCreation treeNode)
			{
				ASTNode anon = treeNode.getAnonymousClassDeclaration();
				if(anon!=null)
				{
					anon.accept(new ASTVisitor(){
						public void endVisit(MethodDeclaration methodNode)
						{
							String className = treeNode.getType().toString();
							String methodName = methodNode.getName().toString();
							localMethods.put(methodName, className);
						}
					});
				}
				return true;
			}

			public void endVisit(TypeDeclaration treeNode)
			{
				classNames.pop();
			}

			public boolean visit(MethodDeclaration treeNode)
			{
				String methodName = treeNode.getName().toString();
				localMethods.put(methodName, classNames.peek());
				return true;
			}
		});
	}

	/*
	 * Checks if a Method called by an Expression is a local method or not.
	 */
	public boolean isLocalMethod(String methodName, Expression expression)
	{
		if(expression == null)
		{
			if(localMethods.containsKey(methodName))
			{
				if(localMethods.get(methodName).contains(classNames.peek()))
					return true;
			}
		}
		else
		{
			if(localMethods.containsKey(methodName))
			{
				if(expression.toString().equals("this"))
					return true;
			}
		}
		return false;
	}

	/*
	 * Checks if a class is a locally defined class.
	 */
	public boolean isLocalClass(String className)
	{
		if(localClasses.contains(className))
			return true;
		else
			return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement)
	 * Extracts candidate classes for variables and populates the maps accordingly 
	 */
	public void endVisit(VariableDeclarationStatement treeNode)
	{
		String treeNodeType = treeNode.getType().toString();
		if(treeNode.getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			treeNodeType = ((ParameterizedType)treeNode.getType()).getType().toString();

		if(!isLocalClass(treeNodeType))
		{
			if(!processedClasses.contains(treeNodeType))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(treeNodeType, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				processedClasses.add(treeNodeType);
			}
			//model.getCandidateClassNodes(treeNodeType, candidateClassNodesCache);
		}
	}

	public boolean visit(EnhancedForStatement treeNode)
	{
		String variableType = treeNode.getParameter().getType().toString();
		if(treeNode.getParameter().getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			variableType = ((ParameterizedType)treeNode.getParameter().getType()).getType().toString();

		if(!isLocalClass(variableType))
		{
			if(!processedClasses.contains(variableType))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(variableType, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(variableType, candidateClassNodesCache);
				processedClasses.add(variableType);
			}
		}
		return true;
	}


	public void endVisit(FieldDeclaration treeNode) 
	{
		String fieldType = null;
		if(treeNode.getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			fieldType = ((ParameterizedType)treeNode.getType()).getType().toString();
		else
			fieldType = treeNode.getType().toString();

		if(!isLocalClass(fieldType))
		{
			if(!processedClasses.contains(fieldType))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(fieldType, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(fieldType, candidateClassNodesCache);
				processedClasses.add(fieldType);
			}
		}
	}

	public boolean visit(TypeDeclaration treeNode)
	{
		classNames.push(treeNode.getName().toString());
		if(treeNode.getSuperclassType() != null)
		{
			if(treeNode.getSuperclassType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
			{
				superclassname = ((ParameterizedType)treeNode.getSuperclassType()).getType().toString();
			}
			else
			{
				superclassname = treeNode.getSuperclassType().toString();
			}
		}

		List<Type> superInterfaces = treeNode.superInterfaceTypes();
		for(Type superInterface : superInterfaces)
		{
			if(superInterface.isParameterizedType())
			{
				interfaces.add(((ParameterizedType)superInterface).getType().toString());
			}
			else if(superInterface.isSimpleType())
			{
				interfaces.add(((SimpleType)superInterface).getName().toString());
			}
			else if(superInterface.isArrayType())
			{
				interfaces.add(((ArrayType)superInterface).getElementType().toString());
			}
			else if(superInterface.isArrayType())
			{
				interfaces.add(((ArrayType)superInterface).getElementType().toString());
			}
		}
		return true;
	}

	public void endVisit(TypeDeclaration treeNode)
	{
		classNames.pop();
	}

	public boolean visit(MethodDeclaration treeNode)
	{
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> param = treeNode.parameters();
		for(int i=0;i<param.size();i++)
		{
			String parameterType = null;
			if(param.get(i).getType().getNodeType() == ASTNode.PARAMETERIZED_TYPE)
				parameterType = ((ParameterizedType)param.get(i).getType()).getType().toString();
			else
				parameterType = param.get(i).getType().toString();

			if(!isLocalClass(parameterType))
			{
				if(!processedClasses.contains(parameterType))
				{
					ThreadedClassFetch tcf = new ThreadedClassFetch(parameterType, candidateClassNodesCache, model);
					classFetchExecutor.execute(tcf);
					//model.getCandidateClassNodes(parameterType, candidateClassNodesCache);
					processedClasses.add(parameterType);
				}
			}
		}

		if(superclassname != null)
		{
			if(!isLocalClass(superclassname))
			{
				if(!processedClasses.contains(superclassname))
				{
					ThreadedClassFetch tcf = new ThreadedClassFetch(superclassname, candidateClassNodesCache, model);
					classFetchExecutor.execute(tcf);
					//model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
					processedClasses.add(superclassname);
				}
			}
		}

		if(!interfaces.isEmpty())
		{
			for(int i=0; i<interfaces.size(); i++)
			{
				if(!isLocalClass(interfaces.get(i).toString()))
				{
					String type = interfaces.get(i).toString();
					if(!processedClasses.contains(type))
					{
						ThreadedClassFetch tcf = new ThreadedClassFetch(type, candidateClassNodesCache, model);
						classFetchExecutor.execute(tcf);
						//model.getCandidateClassNodes(interfaces.get(i).toString(), candidateClassNodesCache);
						processedClasses.add(type);
					}
				}
			}
		}
		return true;
	}

	public boolean visit(CatchClause node)
	{
		if(!isLocalClass(node.getException().getType().toString()))
		{
			String type = node.getException().getType().toString();
			if(!processedClasses.contains(type))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(type, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(node.getException().getType().toString(), candidateClassNodesCache);
				processedClasses.add(type);
			}
		}
		return true;
	}

	public void endVisit(SuperConstructorInvocation treeNode)
	{	
		if(!isLocalClass(superclassname))
		{
			if(!processedClasses.contains(superclassname))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(superclassname, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
				processedClasses.add(superclassname);
			}
		}
	}

	public void endVisit(MethodInvocation treeNode)
	{
		/*String exactName = treeNode.getName().toString();
		if(!isLocalMethod(exactName, treeNode.getExpression()))
		{
			if(!processedMethods.contains(exactName))
			{
				ThreadedMethodFetch tmf = new ThreadedMethodFetch(exactName, candidateMethodNodesCache, methodContainerCache, methodReturnCache, methodParameterCache, model);
				methodFetchExecutor.execute(tmf);
				processedMethods.add(exactName);
			}
		}*/
		
	}
	
	public void endVisit(SuperMethodInvocation treeNode)
	{
		/*if(!isLocalClass(superclassname))
		{
			if(!processedClasses.contains(superclassname))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(superclassname, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(superclassname, candidateClassNodesCache);
				processedClasses.add(superclassname);
			}
		}*/
	}

	public boolean visit(final ClassInstanceCreation treeNode)
	{
		ASTNode anon=treeNode.getAnonymousClassDeclaration();
		if(anon != null)
		{
			anon.accept(new ASTVisitor(){
				public void endVisit(MethodDeclaration md)
				{
					if(!isLocalClass(treeNode.getType().toString()))
					{
						String type = treeNode.getType().toString();
						if(!processedClasses.contains(type))
						{
							ThreadedClassFetch tcf = new ThreadedClassFetch(type, candidateClassNodesCache, model);
							classFetchExecutor.execute(tcf);
							//model.getCandidateClassNodes(treeNode.getType().toString(), candidateClassNodesCache);
							processedClasses.add(type);
						}
					}
				}
			});
		}
		if(!isLocalClass(treeNode.getType().toString()))
		{
			String type = treeNode.getType().toString();
			if(!processedClasses.contains(type))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(type, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(treeNode.getType().toString(), candidateClassNodesCache);
				processedClasses.add(type);
			}
		}
		return true;
	}

	public boolean visit(CastExpression node)
	{
		if(!isLocalClass(node.getType().toString()))
		{
			String type = node.getType().toString();
			if(!processedClasses.contains(type))
			{
				ThreadedClassFetch tcf = new ThreadedClassFetch(type, candidateClassNodesCache, model);
				classFetchExecutor.execute(tcf);
				//model.getCandidateClassNodes(node.getType().toString(), candidateClassNodesCache);
				processedClasses.add(type);
			}
		}

		return true;
	}

}